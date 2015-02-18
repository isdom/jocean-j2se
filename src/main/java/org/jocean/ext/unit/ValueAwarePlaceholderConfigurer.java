package org.jocean.ext.unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.Constants;
import org.springframework.util.Assert;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.util.*;

public class ValueAwarePlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    private static final Logger LOG =
            LoggerFactory.getLogger(ValueAwarePlaceholderConfigurer.class);

    /**
     * Visit each bean definition in the given bean factory and attempt to replace ${...} property
     * placeholders with values from the given properties.
     */
    @Override
    protected void processProperties(final ConfigurableListableBeanFactory beanFactoryToProcess, final Properties props)
            throws BeansException {

        LOG.info("processProperties for beanFactoryToProcess {}", beanFactoryToProcess.toString());

        StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(props);

        this.doProcessProperties(beanFactoryToProcess, valueResolver);
    }

    public Map<String, Object> getResolvedPlaceholders() {
        return Collections.unmodifiableMap(this._resolvedPlaceholders);
    }

    public Map<String, String> getTextedResolvedPlaceholders() {
        return new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;

            {
                for (Map.Entry<String, Object> entry : _resolvedPlaceholders.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        this.put(entry.getKey(), entry.getValue().toString());
                    } else {
                        this.put(entry.getKey(), Arrays.toString((String[]) entry.getValue()));
                    }
                }
            }
        };
    }
    
    public String[] getTextedResolvedPlaceholdersAsStringArray() {
        return new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            {
                for (Map.Entry<String, Object> entry : _resolvedPlaceholders.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        this.add(entry.getKey() + "<--" + entry.getValue().toString());
                    } else {
                        this.add(entry.getKey() + "<--" + Arrays.toString((String[]) entry.getValue()));
                    }
                }
            }
        }.toArray(new String[0]);
    }

    private class PlaceholderResolvingStringValueResolver implements StringValueResolver {

        private final PropertyPlaceholderHelperEx helper;

        private final PropertyPlaceholderHelper.PlaceholderResolver resolver;

        public PlaceholderResolvingStringValueResolver(Properties props) {
            this.helper = new PropertyPlaceholderHelperEx(
                    placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
            this.resolver = new PropertyPlaceholderConfigurerResolver(props);
        }

        public String resolveStringValue(String strVal) throws BeansException {
            String value = this.helper.replacePlaceholders(strVal, this.resolver);
            return (value.equals(nullValue) ? null : value);
        }
    }


    private class PropertyPlaceholderConfigurerResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

        private final Properties props;

        private PropertyPlaceholderConfigurerResolver(Properties props) {
            this.props = props;
        }

        public String resolvePlaceholder(String placeholderName) {
            return ValueAwarePlaceholderConfigurer.this.resolvePlaceholder(placeholderName, props, systemPropertiesMode);
        }
    }

    /**
     * Set the system property mode by the name of the corresponding constant,
     * e.g. "SYSTEM_PROPERTIES_MODE_OVERRIDE".
     *
     * @param constantName name of the constant
     * @throws IllegalArgumentException if an invalid constant was specified
     * @see #setSystemPropertiesMode
     */
    public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
        this.systemPropertiesMode = constants.asNumber(constantName).intValue();
    }

    /**
     * Set how to check system properties: as fallback, as override, or never.
     * For example, will resolve ${user.dir} to the "user.dir" system property.
     * <p>The default is "fallback": If not being able to resolve a placeholder
     * with the specified properties, a system property will be tried.
     * "override" will check for a system property first, before trying the
     * specified properties. "never" will not check system properties at all.
     *
     * @see #SYSTEM_PROPERTIES_MODE_NEVER
     * @see #SYSTEM_PROPERTIES_MODE_FALLBACK
     * @see #SYSTEM_PROPERTIES_MODE_OVERRIDE
     * @see #setSystemPropertiesModeName
     */
    public void setSystemPropertiesMode(int systemPropertiesMode) {
        this.systemPropertiesMode = systemPropertiesMode;
    }

    private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

    private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);

    static {
        wellKnownSimplePrefixes.put("}", "{");
        wellKnownSimplePrefixes.put("]", "[");
        wellKnownSimplePrefixes.put(")", "(");
    }

    private class PropertyPlaceholderHelperEx {
        private final String placeholderPrefix;

        private final String placeholderSuffix;

        private final String simplePrefix;

        private final String valueSeparator;

        private final boolean ignoreUnresolvablePlaceholders;


        /**
         * Creates a new <code>PropertyPlaceholderHelper</code> that uses the supplied prefix and suffix.
         * Unresolvable placeholders are ignored.
         * @param placeholderPrefix the prefix that denotes the start of a placeholder.
         * @param placeholderSuffix the suffix that denotes the end of a placeholder.
         */
        //		public PropertyPlaceholderHelperEx(String placeholderPrefix, String placeholderSuffix) {
        //			this(placeholderPrefix, placeholderSuffix, null, true);
        //		}

        /**
         * Creates a new <code>PropertyPlaceholderHelper</code> that uses the supplied prefix and suffix.
         *
         * @param placeholderPrefix              the prefix that denotes the start of a placeholder
         * @param placeholderSuffix              the suffix that denotes the end of a placeholder
         * @param valueSeparator                 the separating character between the placeholder variable
         *                                       and the associated default value, if any
         * @param ignoreUnresolvablePlaceholders indicates whether unresolvable placeholders should be ignored
         *                                       (<code>true</code>) or cause an exception (<code>false</code>).
         */
        public PropertyPlaceholderHelperEx(String placeholderPrefix, String placeholderSuffix,
                                           String valueSeparator, boolean ignoreUnresolvablePlaceholders) {

            Assert.notNull(placeholderPrefix, "placeholderPrefix must not be null");
            Assert.notNull(placeholderSuffix, "placeholderSuffix must not be null");
            this.placeholderPrefix = placeholderPrefix;
            this.placeholderSuffix = placeholderSuffix;
            String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
            if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
                this.simplePrefix = simplePrefixForSuffix;
            } else {
                this.simplePrefix = this.placeholderPrefix;
            }
            this.valueSeparator = valueSeparator;
            this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
        }


        /**
         * Replaces all placeholders of format <code>${name}</code> with the corresponding property
         * from the supplied {@link java.util.Properties}.
         *
         * @param value      the value containing the placeholders to be replaced.
         * @param properties the <code>Properties</code> to use for replacement.
         * @return the supplied value with placeholders replaced inline.
         */
        public String replacePlaceholders(String value, final Properties properties) {
            Assert.notNull(properties, "Argument 'properties' must not be null.");
            return replacePlaceholders(value, new PropertyPlaceholderHelper.PlaceholderResolver() {
                public String resolvePlaceholder(String placeholderName) {
                    return properties.getProperty(placeholderName);
                }
            });
        }

        /**
         * Replaces all placeholders of format <code>${name}</code> with the value returned from the supplied
         * {@link org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver}.
         *
         * @param value               the value containing the placeholders to be replaced.
         * @param placeholderResolver the <code>PlaceholderResolver</code> to use for replacement.
         * @return the supplied value with placeholders replaced inline.
         */
        public String replacePlaceholders(String value, PropertyPlaceholderHelper.PlaceholderResolver placeholderResolver) {
            Assert.notNull(value, "Argument 'value' must not be null.");
            return parseStringValue(value, placeholderResolver, new HashSet<String>());
        }

        protected String parseStringValue(
                String strVal, PropertyPlaceholderHelper.PlaceholderResolver placeholderResolver, Set<String> visitedPlaceholders) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("in parseStringValue for: {}", strVal);
            }

            StringBuilder buf = new StringBuilder(strVal);

            int startIndex = strVal.indexOf(this.placeholderPrefix);
            while (startIndex != -1) {
                int endIndex = findPlaceholderEndIndex(buf, startIndex);
                if (endIndex != -1) {
                    String placeholder = buf.substring(startIndex + this.placeholderPrefix.length(), endIndex);
                    if (!visitedPlaceholders.add(placeholder)) {
                        throw new IllegalArgumentException(
                                "Circular placeholder reference '" + placeholder + "' in property definitions");
                    }
                    // Recursive invocation, parsing placeholders contained in the placeholder key.
                    placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("found placeholder: {}", placeholder);
                    }
                    // Now obtain the value for the fully resolved key...
                    String propVal = placeholderResolver.resolvePlaceholder(placeholder);
                    if (propVal == null && this.valueSeparator != null) {
                        int separatorIndex = placeholder.indexOf(this.valueSeparator);
                        if (separatorIndex != -1) {
                            String actualPlaceholder = placeholder.substring(0, separatorIndex);
                            String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
                            propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
                            if (propVal == null) {
                                propVal = defaultValue;
                            }
                        }
                    }
                    if (propVal != null) {
                        // Recursive invocation, parsing placeholders contained in the
                        // previously resolved placeholder value.
                        propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
                        buf.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
                        recordResolvedPlaceholder(placeholder, propVal);
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Resolved placeholder '" + placeholder + "'");
                        }
                        startIndex = buf.indexOf(this.placeholderPrefix, startIndex + propVal.length());
                    } else if (this.ignoreUnresolvablePlaceholders) {
                        recordResolvedPlaceholder(placeholder, "");
                        // Proceed with unprocessed value.
                        startIndex = buf.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                    } else {
                        throw new IllegalArgumentException("Could not resolve placeholder '" +
                                placeholder + "'" + " in string value [" + strVal + "]");
                    }

                    visitedPlaceholders.remove(placeholder);
                } else {
                    startIndex = -1;
                }
            }

            return buf.toString();
        }

        private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
            int index = startIndex + this.placeholderPrefix.length();
            int withinNestedPlaceholder = 0;
            while (index < buf.length()) {
                if (StringUtils.substringMatch(buf, index, this.placeholderSuffix)) {
                    if (withinNestedPlaceholder > 0) {
                        withinNestedPlaceholder--;
                        index = index + this.placeholderSuffix.length();
                    } else {
                        return index;
                    }
                } else if (StringUtils.substringMatch(buf, index, this.simplePrefix)) {
                    withinNestedPlaceholder++;
                    index = index + this.simplePrefix.length();
                } else {
                    index++;
                }
            }
            return -1;
        }
    }

    public void recordResolvedPlaceholder(final String placeholder, final String propVal) {
        if (!this._resolvedPlaceholders.containsKey(placeholder)) {
            this._resolvedPlaceholders.put(placeholder, propVal);
        } else {
            final Object data = this._resolvedPlaceholders.get(placeholder);
            if (data instanceof String) {
                if (data.equals(propVal)) {
                    //	just ignore
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("found the same propVal {} for placeholder {}, just ignore", propVal, placeholder);
                    }
                } else {
                    this._resolvedPlaceholders.put(placeholder, new String[]{propVal, data.toString()});
                }
            } else {
                // suppose string arrays
                final String[] arrays = (String[]) data;
                if (Arrays.asList(arrays).contains(propVal)) {
                    //	just ignore
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("found the same propVal {} for placeholder {}, just ignore", propVal, placeholder);
                    }
                } else {
                    final String[] newArrays = Arrays.copyOf(arrays, arrays.length + 1);
                    newArrays[arrays.length] = propVal;
                    this._resolvedPlaceholders.put(placeholder, newArrays);
                }
            }
        }
    }

    private final Map<String, Object> _resolvedPlaceholders = new HashMap<>();
}
