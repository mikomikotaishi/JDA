rootProject.name = "JDA"

includeBuild("formatter-recipes") {
    dependencySubstitution {
        substitute(module("net.dv8tion.jda:formatter-recipes")).using(project(":"))
    }
}
