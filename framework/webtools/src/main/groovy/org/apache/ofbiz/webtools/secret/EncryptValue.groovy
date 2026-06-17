import org.apache.ofbiz.base.secret.SecretProviderFactory
import org.apache.ofbiz.base.secret.SecretValueResolver
import org.apache.ofbiz.base.util.UtilProperties

context.SecretValueMarker = SecretValueResolver.MARKER_NAME
context.activeSecretProvider = SecretProviderFactory.getProviderName()
def props = UtilProperties.getProperties("security")
context.activeSettings = [
    "Lookup marker"      : props?.getProperty("secret.value.marker", "LOOKUP"),
    "Cache TTL (seconds)": props?.getProperty("secret.cache.ttl.seconds", "300"),
    "Retry count"        : props?.getProperty("secret.provider.retry.count", "2"),
    "Retry delay (ms)"   : props?.getProperty("secret.provider.retry.delay.ms", "500"),
    "Master key env var" : props?.getProperty("secret.master.key.env.var", "OFBIZ_MASTER_KEY"),
    "PBKDF2 iterations"  : props?.getProperty("secret.pbkdf2.iterations", "310000")
]

// getSecretUsageStats stores its OUT params as request attributes; pull them into the
// screen context since populateBasicContext() only exposes request attrs under "parameters".
if (parameters.usageSummary) {
    context.usageSummary = parameters.usageSummary
}
if (parameters.usageReport) {
    context.usageReport = parameters.usageReport
}
