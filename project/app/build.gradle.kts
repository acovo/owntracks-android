import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.File
import java.util.Properties

plugins {
  id("com.android.application")
  id("com.google.dagger.hilt.android")
  kotlin("android")
  kotlin("kapt")
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.ksp)
}

apply<EspressoMetadataEmbeddingPlugin>()

val googleMapsAPIKey =
    System.getenv("GOOGLE_MAPS_API_KEY")?.toString()
        ?: extra.get("google_maps_api_key")?.toString()
        ?: "PLACEHOLDER_API_KEY"

// 读取local.properties文件
val localProperties = File(rootDir, "local.properties")
val bmapAPIKey = if (localProperties.exists()) {
    val props = Properties()
    localProperties.inputStream().use { props.load(it) }
    System.getenv("BMAP_API_KEY")?.toString() ?: props.getProperty("bmap.api.key", "PLACEHOLDER_BMAP_API_KEY")
} else {
    System.getenv("BMAP_API_KEY")?.toString() ?: "PLACEHOLDER_BMAP_API_KEY"
}

// 读取mqtt.host配置
val mqttHost = if (localProperties.exists()) {
    val props = Properties()
    localProperties.inputStream().use { props.load(it) }
    System.getenv("MQTT_HOST")?.toString() ?: props.getProperty("mqtt.host", "")
} else {
    System.getenv("MQTT_HOST")?.toString() ?: ""
}

// 读取mqtt.port配置
val mqttPort = if (localProperties.exists()) {
    val props = Properties()
    localProperties.inputStream().use { props.load(it) }
    System.getenv("MQTT_PORT")?.toInt() ?: props.getProperty("mqtt.port", "1883").toInt()
} else {
    System.getenv("MQTT_PORT")?.toInt() ?: 1883
}

// 读取keepalive配置
val keepalive = if (localProperties.exists()) {
    val props = Properties()
    localProperties.inputStream().use { props.load(it) }
    System.getenv("KEEPALIVE")?.toInt() ?: props.getProperty("keepalive", "30").toInt()
} else {
    System.getenv("KEEPALIVE")?.toInt() ?: 30
}

// 读取locatorDisplacement配置
val locatorDisplacement = if (localProperties.exists()) {
    val props = Properties()
    localProperties.inputStream().use { props.load(it) }
    System.getenv("LOCATOR_DISPLACEMENT")?.toInt() ?: props.getProperty("locatorDisplacement", "10").toInt()
} else {
    System.getenv("LOCATOR_DISPLACEMENT")?.toInt() ?: 10
}

// 读取cmd配置
val cmd = if (localProperties.exists()) {
    val props = Properties()
    localProperties.inputStream().use { props.load(it) }
    System.getenv("CMD")?.toBoolean() ?: props.getProperty("cmd", "true").toBoolean()
} else {
    System.getenv("CMD")?.toBoolean() ?: true
}

// 读取remoteConfiguration配置
val remoteConfiguration = if (localProperties.exists()) {
    val props = Properties()
    localProperties.inputStream().use { props.load(it) }
    System.getenv("REMOTE_CONFIGURATION")?.toBoolean() ?: props.getProperty("remoteConfiguration", "true").toBoolean()
} else {
    System.getenv("REMOTE_CONFIGURATION")?.toBoolean() ?: true
}

val gmsImplementation: Configuration by configurations.creating

val versionNameValue = "2.5.6"

fun generateVersionCode(versionName: String): Int {
  val parts = versionName.split(".")
  val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
  val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
  val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
  return 400000000 + major * 10000000 + minor * 100000 + patch * 1000
}

val generatedVersionCode = generateVersionCode(versionNameValue)
val envVersionCode = System.getenv("VERSION_CODE")?.toInt()
val packageVersionCode: Int =
    if (envVersionCode != null && envVersionCode > generatedVersionCode) {
      envVersionCode
    } else {
      generatedVersionCode
    }
val manuallySetVersion: Boolean = System.getenv("VERSION_CODE") != null
val enablePlayPublishing: Boolean = !System.getenv("ANDROID_PUBLISHER_CREDENTIALS").isNullOrBlank()

android {
  compileSdk = 36
  namespace = "org.owntracks.android"

  defaultConfig {
    applicationId = "org.owntracks.android"
    minSdk = 24
    targetSdk = 36

    versionCode = packageVersionCode
    versionName = versionNameValue

    val localeCount = fileTree("src/main/res/").matching { include("**/strings.xml") }.files.size

    buildConfigField(
        "int",
        "TRANSLATION_COUNT",
        localeCount.toString(),
    )
    
    // 添加MQTT_HOST配置
    buildConfigField(
        "String",
        "MQTT_HOST",
        "\"$mqttHost\"",
    )
    
    // 添加MQTT_PORT配置
    buildConfigField(
        "int",
        "MQTT_PORT",
        "$mqttPort",
    )
    
    // 添加KEEPALIVE配置
    buildConfigField(
        "int",
        "KEEPALIVE",
        "$keepalive",
    )
    
    // 添加LOCATOR_DISPLACEMENT配置
    buildConfigField(
        "int",
        "LOCATOR_DISPLACEMENT",
        "$locatorDisplacement",
    )
    
    // 添加CMD配置
    buildConfigField(
        "boolean",
        "CMD",
        "$cmd",
    )
    
    // 添加REMOTE_CONFIGURATION配置
    buildConfigField(
        "boolean",
        "REMOTE_CONFIGURATION",
        "$remoteConfiguration",
    )

    testInstrumentationRunner = "org.owntracks.android.testutils.hilt.CustomTestRunner"

    testInstrumentationRunnerArguments.putAll(
        mapOf(
            "clearPackageData" to "false",
            "coverage" to "true",
            "disableAnalytics" to "true",
            "useTestStorageService" to "false",
        ),
    )
    javaCompileOptions {
      annotationProcessorOptions { arguments["room.schemaLocation"] = "$projectDir/schemas" }
    }
  }

  androidResources { generateLocaleConfig = true }

  signingConfigs {
    // 检查是否已经有debug签名配置，如果没有就创建
    val debugSigningConfig = findByName("debug")
    if (debugSigningConfig != null) {
      debugSigningConfig.apply {
        keyAlias = "androiddebugkey"
        keyPassword = "android"
        storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
        storePassword = "android"
        enableV1Signing = true
        enableV2Signing = true
      }
    } else {
      create("debug") {
        keyAlias = "androiddebugkey"
        keyPassword = "android"
        storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
        storePassword = "android"
        enableV1Signing = true
        enableV2Signing = true
      }
    }
    
    if (!System.getenv("KEYSTORE_PASSPHRASE").isNullOrBlank()) {
      register("release") {
        keyAlias = "upload"
        keyPassword = System.getenv("KEYSTORE_PASSPHRASE")
        storeFile = file("../owntracks.release.keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSPHRASE")
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
        enableV4Signing = true
      }
    }
  }

  buildTypes {
    named("release") {
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles.addAll(
          listOf(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              file("proguard-rules.pro"),
          ),
      )
      resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
      signingConfig = signingConfigs.findByName("debug")
    }

    named("debug") {
      isMinifyEnabled = false
      isShrinkResources = false
      isPseudoLocalesEnabled = true
      proguardFiles.addAll(
          listOf(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              file("proguard-rules.pro"),
          ),
      )
      // 使用已有的debug签名配置
      signingConfig = signingConfigs.findByName("debug")
      resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
      applicationIdSuffix = ".debug"
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
  }

  buildFeatures {
    buildConfig = true
    dataBinding = true
    viewBinding = true
  }

  dataBinding { addKtx = true }

  packaging {
    resources.excludes.add("META-INF/*")
    jniLibs.useLegacyPackaging = false
  }

  lint {
    baseline = file("../../lint/lint-baseline.xml")
    lintConfig = file("../../lint/lint.xml")
    checkAllWarnings = true
    warningsAsErrors = false
    abortOnError = false
    disable.addAll(
        setOf(
            "TypographyFractions",
            "TypographyQuotes",
            "Typos",
        ),
    )
  }
  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    animationsDisabled = true
    unitTests { isIncludeAndroidResources = true }
    managedDevices {
      localDevices {}
      groups {}
    }
  }

  tasks.withType<Test> {
    testLogging {
      events(
          TestLogEvent.SKIPPED,
          TestLogEvent.FAILED,
          TestLogEvent.STANDARD_OUT,
          TestLogEvent.STANDARD_ERROR,
      )
      setExceptionFormat("full")
      showStandardStreams = true

      showCauses = true
      showExceptions = true
      showStackTraces = true
    }
    reports.junitXml.required.set(true)
    reports.html.required.set(true)
    outputs.upToDateWhen { false }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    isCoreLibraryDesugaringEnabled = true
  }

  kotlinOptions { jvmTarget = JavaVersion.VERSION_17.toString() }

  flavorDimensions.add("locationProvider")
  productFlavors {
    create("gms") {
      dimension = "locationProvider"
      dependencies {
        gmsImplementation(libs.gms.play.services.maps)
        gmsImplementation(libs.play.services.location)
      }
    }
    create("oss") {
      dimension = "locationProvider"
      manifestPlaceholders["bmap.api.key"] = bmapAPIKey
      dependencies {
        // 百度地图SDK依赖配置
        implementation("com.baidu.lbsyun:BaiduMapSDK_Map:7.6.4")
        implementation("com.baidu.lbsyun:BaiduMapSDK_Search:7.6.4")
        implementation("com.baidu.lbsyun:BaiduMapSDK_Util:7.6.4")
      }
    }
  }
}

kapt {
  useBuildCache = true
  correctErrorTypes = true
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

tasks.withType<Test> {
  systemProperties["junit.jupiter.execution.parallel.enabled"] = false
  systemProperties["junit.jupiter.execution.parallel.mode.default"] = "same_thread"
  systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
  maxParallelForks = 1
}

tasks.withType<JavaCompile>().configureEach { options.isFork = true }

dependencies {
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.androidx)
  implementation(libs.androidx.test.espresso.idling)

  implementation(libs.google.material)

  // Explicit dependency on conscrypt to give up-to-date TLS support on all devices
  implementation(libs.conscrypt)

  // Mapping
  implementation(libs.osmdroid)
  // 百度地图SDK依赖暂时注释，使用本地AAR文件（已移至oss风味的dependencies块中）
  // implementation(libs.baidu.map.sdk)
  // implementation(libs.baidu.map.utils)

  // Connectivity
  implementation(libs.paho.mqttclient)
  implementation(libs.okhttp)

  // Utility libraries
  implementation(libs.bundles.hilt)
  implementation(libs.bundles.jackson)
  implementation(libs.square.tape2)
  implementation(libs.timber)
  implementation(libs.apache.httpcore)
  implementation(libs.bundles.androidx.room)
  implementation(libs.bundles.objectbox.migration)
  implementation(libs.kotlin.datetime)
  implementation(libs.kotlin.serialization)

  // The BC version shipped under com.android is half-broken. Weird certificate issues etc.
  // To solve, we bring in our own version of BC
  implementation(libs.bouncycastle)

  // Widget libraries
  implementation(libs.widgets.materialize) { artifact { type = "aar" } }

  // These Java EE libs are no longer included in JDKs, so we include explicitly
  kapt(libs.bundles.jaxb.annotation.processors)

  // Preprocessors
  kapt(libs.bundles.kapt.hilt)
  ksp(libs.androidx.room.compiler)

  kaptTest(libs.bundles.kapt.hilt)

  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.androidx.core.testing)
  testImplementation(libs.kotlin.coroutines.test)

  androidTestImplementation(libs.bundles.androidx.test)

  // Hilt Android Testing
  androidTestImplementation(libs.hilt.android.testing)
  kaptAndroidTest(libs.hilt.compiler)

  androidTestImplementation(libs.barista) { exclude("org.jetbrains.kotlin") }
  androidTestImplementation(libs.okhttp.mockwebserver)
  androidTestImplementation(libs.bundles.kmqtt)
  androidTestImplementation(libs.square.leakcanary)

  androidTestUtil(libs.bundles.androidx.test.util)

  coreLibraryDesugaring(libs.desugar)
}
