package com.fasterxml.jackson.jr.ob.impl;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.TokenStreamFactory;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.api.ReaderWriterProvider;
import com.fasterxml.jackson.jr.ob.api.ValueReader;
import com.fasterxml.jackson.jr.type.ResolvedType;
import com.fasterxml.jackson.jr.type.TypeBindings;
import com.fasterxml.jackson.jr.type.TypeResolver;

/**
 * Helper object used for efficient detection of type information
 * relevant to our conversion needs when writing out Java Objects
 * as JSON.
 *<p>
 * Note that usage pattern is such that a single "root" instance is kept
 * by each {@link com.fasterxml.jackson.jr.ob.JSON} instance; and
 * an actual per-operation instance must be constructed by calling
 * {@link #perOperationInstance}: reason for this is that instances
 * use simple caching to handle the common case of repeating types
 * within JSON Arrays.
 */
public class ValueReaderLocator
    extends ValueLocatorBase
{
    /**
     * While we should be able to cache all types in the active working set,
     * we should also avoid potential unbounded retention, since there is
     * often just one big instance per JVM (or at least ClassLoader).
     */
    protected final static int MAX_CACHED_READERS = 500;

    /*
    /**********************************************************************
    /* Helper objects
    /**********************************************************************
     */

    /**
     * We need stream factory for constructing {@link FieldNameMatcher}s for
     * POJO deserializer.
     *
     * @since 3.0
     */
    protected TokenStreamFactory _streamFactory;
    
    /**
     * For generic containers (Collections, Maps, arrays), we may need
     * this guy.
     */
    protected final TypeResolver _typeResolver;

    /**
     * Provider for custom readers, if any; may be null.
     *
     * @since 2.10
     */
    protected final ReaderWriterProvider _readerProvider;

    /*
    /**********************************************************************
    /* Caching
    /**********************************************************************
     */
    
    /**
     * Set of {@link ValueReader}s that we have resolved
     */
    protected final ConcurrentHashMap<ClassKey, ValueReader> _knownReaders;

    /**
     * During resolution, some readers may be in-progress, but need to be
     * linked: for example, with cyclic type references.
     */
    protected Map<ClassKey, ValueReader> _incompleteReaders;

    /**
     * Object used for mutex during construction of a Bean deserializer: necessary
     * to avoid race conditions during handling of cyclic dependencies.
     */
    protected final Object _readerLock;

    /*
    /**********************************************************************
    /* Instance configuration
    /**********************************************************************
     */

    /**
     * Feature flags that are enabled
     */
    protected final int _features;

    protected final JSONReader _readContext;

    /*
    /**********************************************************************
    /* Instance state, caching
    /**********************************************************************
     */
    
    /**
     * Reusable lookup key; only used by per-thread instances.
     */
    private ClassKey _key;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    /**
     * Constructor for the blueprint instance
     */
    protected ValueReaderLocator(TokenStreamFactory streamF, int features,
            ReaderWriterProvider rwp)
    {
        _streamFactory = streamF;
        _features = features;
        _readerProvider = rwp;
        _knownReaders = new ConcurrentHashMap<ClassKey, ValueReader>(10, 0.75f, 2);
        _typeResolver = new TypeResolver();
        _readerLock = new Object();
        _readContext = null;
    }

    protected ValueReaderLocator(ValueReaderLocator base, int features, JSONReader r) {
        _streamFactory = base._streamFactory;
        _features = features;
        _readContext = r;
        _readerProvider = base._readerProvider;
        _knownReaders = base._knownReaders;
        _typeResolver = base._typeResolver;
        _readerLock = base._readerLock;
    }

    protected ValueReaderLocator(ValueReaderLocator base, ReaderWriterProvider rwp) {
        _streamFactory = base._streamFactory;
        _features = base._features;
        _readContext = base._readContext;
        _readerProvider = rwp;
        // create new cache as there may be custom writers:
        _knownReaders = new ConcurrentHashMap<ClassKey, ValueReader>(10, 0.75f, 2);
        _typeResolver = base._typeResolver;
        _readerLock = base._readerLock;
    }
    
    public final static ValueReaderLocator blueprint(TokenStreamFactory streamF,
            int features, ReaderWriterProvider rwp) {
        return new ValueReaderLocator(streamF, features & CACHE_FLAGS, rwp);
    }

    public ValueReaderLocator with(ReaderWriterProvider rwp) {
        if (rwp == _readerProvider) {
            return this;
        }
        return new ValueReaderLocator(this, rwp);
    }

    public ValueReaderLocator perOperationInstance(JSONReader r, int features) {
        return new ValueReaderLocator(this, features & CACHE_FLAGS, r);
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */
    
    /**
     * Method used during deserialization to find handler for given
     * non-generic type: will first check for already resolved (and cached) readers
     * -- and return if one found -- and then if no cached reader found, create
     * one, cache, return.
     *
     * @param raw Type-erased type of value to find reader for
     *
     * @return ValueReader to use for given type
     */
    public ValueReader findReader(Class<?> raw)
    {
        ClassKey k = (_key == null) ? new ClassKey(raw, _features) : _key.with(raw, _features);
        ValueReader vr = _knownReaders.get(k);
        if (vr != null) {
            return vr;
        }
        vr = createReader(null, raw, raw);
        // 15-Jun-2016, tatu: Let's limit maximum number of readers to prevent
        //   unbounded memory retention (at least wrt readers)
        if (_knownReaders.size() >= MAX_CACHED_READERS) {
            _knownReaders.clear();
        }
        _knownReaders.putIfAbsent(new ClassKey(raw, _features), vr);
        return vr;
    }

    /**
     * Factory method for creating standard readers of any declared type.
     *
     * @param contextType Context for resolving generic type parameters
     * @param type Type-erased type of value to construct reader for
     * @param genericType Full (possibly) generic type of value to construct reader for (important
     *   for {@link java.util.Map}, {@link java.util.Collection}).
     */
    protected ValueReader createReader(Class<?> contextType, Class<?> type, Type genericType)
    {
        if (type == Object.class) {
            return AnyReader.std;
        }
        if (type.isArray()) {
            return arrayReader(contextType, type);
        }
        if (type.isEnum()) {
            if (_readerProvider != null) {
                ValueReader r = _readerProvider.findValueReader(_readContext, type);
                if (r != null) {
                    return r;
                }
            }
           return enumReader(type);
        }
        if (Collection.class.isAssignableFrom(type)) {
            return collectionReader(contextType, genericType);
        }
        if (Map.class.isAssignableFrom(type)) {
            return mapReader(contextType, genericType);
        }
        // Unlike with other types, check custom handler here before
        // simple type check, to allow overriding handling of `String` etc
        if (_readerProvider != null) {
            ValueReader r = _readerProvider.findValueReader(_readContext, type);
            if (r != null) {
                return r;
            }
        }
        int typeId = _findSimpleType(type, false);
        if (typeId > 0) {
            return new SimpleValueReader(type, typeId);
        }
        return beanReader(type);
    }

    /*
    /**********************************************************************
    /* Factory methods for non-Bean readers
    /**********************************************************************
     */

    protected ValueReader arrayReader(Class<?> contextType, Class<?> arrayType) {
        // TODO: maybe allow custom array readers?
        Class<?> elemType = arrayType.getComponentType();
        if (!elemType.isPrimitive()) {
            return new ArrayReader(arrayType, elemType,
                    createReader(contextType, elemType, elemType));
        }
        int typeId = _findSimpleType(arrayType, false);
        if (typeId > 0) {
            return new SimpleValueReader(arrayType, typeId);
        }
        throw new IllegalArgumentException("Deserialization of "+arrayType.getName()+" not (yet) supported");
    }

    protected ValueReader enumReader(Class<?> enumType) {
        Object[] enums = enumType.getEnumConstants();
        Map<String,Object> byName = new HashMap<String,Object>();
        for (Object e : enums) {
            byName.put(e.toString(), e);
        }
        return new EnumReader(enumType, enums, byName);
    }

    protected ValueReader collectionReader(Class<?> contextType, Type collectionType)
    {
        ResolvedType t = _typeResolver.resolve(_bindings(contextType), collectionType);
        List<ResolvedType> params = t.typeParametersFor(Collection.class);
        return collectionReader(t.erasedType(), params.get(0));
    }

    protected ValueReader collectionReader(Class<?> collectionType, ResolvedType valueType)
    {
        final Class<?> rawValueType = valueType.erasedType();
        final ValueReader valueReader;
        if (Collection.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Collection.class);
            valueReader = collectionReader(rawValueType, params.get(0));
        } else if (Map.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Map.class);
            valueReader = mapReader(rawValueType, params.get(1));
        } else {
            valueReader = findReader(rawValueType);
        }
        if (_readerProvider != null) {
            ValueReader r = _readerProvider.findCollectionReader(_readContext,
                    collectionType, valueType, valueReader);
            if (r != null) {
                return r;
            }
        }

        return new CollectionReader(collectionType, valueReader);
    }

    protected ValueReader mapReader(Class<?> contextType, Type mapType)
    {
        ResolvedType t = _typeResolver.resolve(_bindings(contextType), mapType);
        List<ResolvedType> params = t.typeParametersFor(Map.class);
        return mapReader(t.erasedType(), params.get(1));
    }

    protected ValueReader mapReader(Class<?> mapType, ResolvedType valueType)
    {
        final Class<?> rawValueType = valueType.erasedType();
        final ValueReader valueReader;
        if (Collection.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Collection.class);
            valueReader = collectionReader(rawValueType, params.get(0));
        } else if (Map.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Map.class);
            valueReader = mapReader(rawValueType, params.get(1));
        } else {
            valueReader = findReader(rawValueType);
        }
        if (_readerProvider != null) {
            ValueReader r = _readerProvider.findMapReader(_readContext,
                    mapType, valueType, valueReader);
            if (r != null) {
                return r;
            }
        }
        return new MapReader(mapType, valueReader);
    }

    protected ValueReader beanReader(Class<?> type)
    {
        // NOTE: caller (must) handles custom reader lookup earlier, not done here

        final ClassKey key = new ClassKey(type, _features);
        synchronized (_readerLock) {
            if (_incompleteReaders == null) {
                _incompleteReaders = new HashMap<ClassKey, ValueReader>();
            } else { // perhaps it has already been resolved?
                ValueReader vr = _incompleteReaders.get(key);
                if (vr != null) {
                    return vr;
                }
            }
            BeanReader def = _resolveBeanForDeser(type);
            try {
                _incompleteReaders.put(key, def);
                for (Map.Entry<String, BeanPropertyReader> entry : def.propertiesByName().entrySet()) {
                    BeanPropertyReader prop = entry.getValue();
                    entry.setValue(prop.withReader(createReader(type,
                            prop.rawSetterType(), prop.genericSetterType())));
                }
                def.initFieldMatcher(_streamFactory);
            } finally {
                _incompleteReaders.remove(key);
            }
            return def;
        }
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected BeanReader _resolveBeanForDeser(Class<?> raw)
    {
        final POJODefinition pojoDef = _resolveBeanDef(raw);

        Constructor<?> defaultCtor = pojoDef.defaultCtor;
        Constructor<?> stringCtor = pojoDef.stringCtor;
        Constructor<?> longCtor = pojoDef.longCtor;

        final boolean forceAccess = JSON.Feature.FORCE_REFLECTION_ACCESS.isEnabled(_features);
        if (forceAccess) {
            if (defaultCtor != null) {
                defaultCtor.setAccessible(true);
            }
            if (stringCtor != null) {
                stringCtor.setAccessible(true);
            }
            if (longCtor != null) {
                longCtor.setAccessible(true);
            }
        }

        final POJODefinition.Prop[] rawProps = pojoDef.properties();
        final int len = rawProps.length;
        final Map<String, BeanPropertyReader> propMap;
        if (len == 0) {
            propMap = Collections.emptyMap();
        } else {
            propMap = new HashMap<String, BeanPropertyReader>();
            final boolean useFields = JSON.Feature.USE_FIELDS.isEnabled(_features);
            for (int i = 0; i < len; ++i) {
                POJODefinition.Prop rawProp = rawProps[i];                
                Method m = rawProp.setter;
                Field f = useFields ? rawProp.field : null;

                if (m != null) {
                    if (forceAccess) {
                        m.setAccessible(true);
                    } else if (!Modifier.isPublic(m.getModifiers())) {
                        // access to non-public setters must be forced to be usable:
                        m = null;
                    }
                }
                // if no setter, field would do as well
                if (m == null) {
                    if (f == null) {
                        continue;
                    }
                    // fields should always be public, but let's just double-check
                    if (forceAccess) {
                        f.setAccessible(true);
                    } else if (!Modifier.isPublic(f.getModifiers())) {
                        continue;
                    }
                }
                propMap.put(rawProp.name, new BeanPropertyReader(rawProp.name, f, m));
            }
        }
        return BeanReader.construct(raw, propMap, defaultCtor, stringCtor, longCtor);
    }

    private TypeBindings _bindings(Class<?> ctxt) {
        if (ctxt == null) {
            return TypeBindings.emptyBindings();
        }
        return TypeBindings.create(ctxt, (ResolvedType[]) null);
    }
}
