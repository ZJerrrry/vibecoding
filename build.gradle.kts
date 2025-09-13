plugins {
    id("java")
    application
    // 添加JavaFX插件
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // 引入JavaCV平台，它包含了OpenCV并能更好地处理原生依赖
    implementation("org.bytedeco:javacv-platform:1.5.7")

    // JavaFX依赖保持不变
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("org.example.Main")
}

// JavaFX的配置
javafx {
    version = "17.0.2"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing") // swing是为了将摄像头图像转为JavaFX图像
}

tasks.test {
    useJUnitPlatform()
}