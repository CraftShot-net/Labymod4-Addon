plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
}

val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "me.timeox2k"
version = providers.environmentVariable("VERSION").getOrElse("1.0.3")

labyMod {
    defaultPackageName = "me.timeox2k.craftshot"

    addonInfo {
        namespace = "craftshot"
        displayName = "CraftShot"
        author = "Timeox2k"
        description = "CraftShot - The Social Network for your Screenshots. Just Screenshot to upload it to CraftShot."
        minecraftVersion = "*"
        version = rootProject.version.toString()
    }

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    devLogin = true
                }
            }
        }
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version
}