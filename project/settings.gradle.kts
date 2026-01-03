pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    // 百度地图SDK仓库
    maven {
      url = uri("https://maven.baidu.com/")
      isAllowInsecureProtocol = true
    }
  }
}

include(":app")

rootProject.name = "owntracks-android"

include(":location-kalman")
