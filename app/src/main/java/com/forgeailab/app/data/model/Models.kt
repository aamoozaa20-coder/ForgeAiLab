package com.forgeailab.app.data.model

data class ApkInfo(
    val packageName: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
    val compileSdk: Int = 0,
    val appLabel: String = "",
    val fileSize: Long = 0,
    val filePath: String = "",
    val fileName: String = "",
    val md5: String = "",
    val sha1: String = "",
    val sha256: String = "",
    val permissions: List<String> = emptyList(),
    val activities: List<ComponentInfo> = emptyList(),
    val services: List<ComponentInfo> = emptyList(),
    val receivers: List<ComponentInfo> = emptyList(),
    val providers: List<ComponentInfo> = emptyList(),
    val features: List<String> = emptyList(),
    val libraries: List<String> = emptyList(),
    val nativeLibs: List<NativeLibInfo> = emptyList(),
    val dexFiles: List<DexInfo> = emptyList(),
    val resources: List<ResourceEntry> = emptyList(),
    val assets: List<AssetEntry> = emptyList(),
    val knownSdks: List<DetectedSdk> = emptyList(),
    val strings: List<ExtractedString> = emptyList(),
    val manifestRaw: String = "",
    val analyzedAt: Long = System.currentTimeMillis(),
    val analysisNotes: List<String> = emptyList()
)

data class ComponentInfo(
    val name: String,
    val exported: Boolean = false,
    val enabled: Boolean = true,
    val process: String? = null,
    val permission: String? = null,
    val intentFilters: List<String> = emptyList()
)

data class NativeLibInfo(val path: String, val abi: String, val size: Long)
data class DexInfo(val name: String, val size: Long, val classCountEstimate: Int = 0)
data class ResourceEntry(val path: String, val type: ResourceType, val size: Long, val density: String? = null)
enum class ResourceType { DRAWABLE, LAYOUT, VALUES, MIPMAP, RAW, XML, ANIM, COLOR, FONT, OTHER }
data class AssetEntry(val path: String, val size: Long)
data class DetectedSdk(val name: String, val category: SdkCategory, val evidence: List<String>, val confidence: Float)
enum class SdkCategory { ADS, ANALYTICS, NETWORK, SOCIAL, PAYMENT, PUSH, CRASH, MAP, AUTH, OTHER }
data class ExtractedString(val value: String, val source: String, val isUiLikely: Boolean = false)

data class WorkspaceProject(
    val id: String,
    val name: String,
    val originalApkPath: String,
    val workDir: String,
    val apkInfo: ApkInfo?,
    val createdAt: Long,
    val updatedAt: Long,
    val changeLog: List<ChangeRecord> = emptyList()
)

data class ChangeRecord(
    val id: String,
    val timestamp: Long,
    val description: String,
    val type: ChangeType,
    val path: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null
)
enum class ChangeType { STRING_EDIT, IMAGE_REPLACE, MANIFEST_EDIT, FILE_ADD, FILE_DELETE, RESOURCE_EDIT, OTHER }

data class RebuildResult(val success: Boolean, val outputPath: String? = null, val steps: List<RebuildStep> = emptyList(), val errors: List<String> = emptyList(), val signed: Boolean = false)
data class RebuildStep(val name: String, val status: StepStatus, val message: String = "", val durationMs: Long = 0)
enum class StepStatus { PENDING, RUNNING, SUCCESS, FAILED, SKIPPED }
data class SignResult(val success: Boolean, val certificateInfo: String = "", val message: String = "", val outputPath: String? = null)
data class CompareResult(val packageMatch: Boolean, val versionDiff: String, val permissionAdded: List<String>, val permissionRemoved: List<String>, val componentChanges: List<String>, val sizeDiff: Long, val hashMatch: Boolean, val summary: String)
data class AnalysisReport(val generatedAt: Long, val apkInfo: ApkInfo, val html: String, val json: String)
data class AiMessage(val id: String, val role: AiRole, val content: String, val timestamp: Long = System.currentTimeMillis(), val toolCalls: List<String> = emptyList())
enum class AiRole { USER, ASSISTANT, SYSTEM, TOOL }
data class AiToolResult(val toolName: String, val success: Boolean, val output: String, val data: Any? = null)
