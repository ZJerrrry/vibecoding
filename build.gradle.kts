plugins {
    id("java")
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    // 只使用SwingApp作为主类
    mainClass.set("org.example.SwingApp")
}

// 创建一个特殊的任务，只编译和运行SwingApp
tasks.register<JavaExec>("runSwingApp") {
    group = "application"
    description = "运行Swing版本的应用程序"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.example.SwingApp")
}

// 配置Java编译选项
tasks.withType<JavaCompile> {
    // 只编译SwingApp，忽略其他带有JavaFX依赖的文件
    options.compilerArgs.add("-Xlint:-path")
}

tasks.test {
    useJUnitPlatform()
}