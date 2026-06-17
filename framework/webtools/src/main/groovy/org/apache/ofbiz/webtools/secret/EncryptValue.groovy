import org.apache.ofbiz.base.secret.SecretProviderFactory
import org.apache.ofbiz.base.secret.SecretValueResolver
import org.apache.ofbiz.base.util.UtilProperties

context.SecretValueMarker = SecretValueResolver.MARKER_NAME
context.activeSecretProvider = SecretProviderFactory.getProviderName()
Properties props = UtilProperties.getProperties('security')
// A List of [label, value] pairs, not a Map: FreeMarker's BeansWrapper exposes java.util.Map's
// own methods (getClass, keySet, replace, ...) as extra "keys" alongside real entries when a
// Map is iterated with ?keys/[k], so the template renders garbage for those bogus pseudo-keys.
// A List is wrapped as a plain sequence with no such method-name leakage.
context.activeSettings = [
    ['Lookup marker', props?.getProperty('secret.value.marker', 'LOOKUP')],
    ['Cache TTL (seconds)', props?.getProperty('secret.cache.ttl.seconds', '300')],
    ['Retry count', props?.getProperty('secret.provider.retry.count', '2')],
    ['Retry delay (ms)', props?.getProperty('secret.provider.retry.delay.ms', '500')],
    ['Master key env var', props?.getProperty('secret.master.key.env.var', 'OFBIZ_MASTER_KEY')],
    ['PBKDF2 iterations', props?.getProperty('secret.pbkdf2.iterations', '310000')]
]

// getSecretUsageStats stores its OUT params as request attributes; pull them into the
// screen context since populateBasicContext() only exposes request attrs under "parameters".
if (parameters.usageSummary) {
    context.usageSummary = parameters.usageSummary
}
if (parameters.usageReport) {
    // Same Map-iteration pitfall as activeSettings above: flatten to a List of rows before
    // handing it to the template instead of iterating the Map with ?keys/[key].
    context.usageReportRows = parameters.usageReport.collect { key, stats ->
        [key, stats.hits ?: 0, stats.misses ?: 0, stats.total ?: 0]
    }
}
