package nl.rubensten.texifyidea.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.roots.ProjectRootManager
import nl.rubensten.texifyidea.settings.TexFlavor
import nl.rubensten.texifyidea.settings.TexifySettings

/**
 * @author Sten Wessel
 */
open class BibtexCommandLineState(
        environment: ExecutionEnvironment,
        private val runConfig: BibtexRunConfiguration
) : CommandLineState(environment) {

    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
        val compiler = runConfig.compiler ?: throw ExecutionException("No valid compiler specified.")
        val command: List<String> = compiler.getCommand(runConfig, environment.project) ?: throw ExecutionException("Compile command could not be created.")
        val moduleRoots = ProjectRootManager.getInstance(environment.project).contentSourceRoots

        val commandLine = GeneralCommandLine(command).withWorkDirectory(runConfig.auxDir?.path ?: runConfig.mainFile?.parent?.path)
        val handler: ProcessHandler = KillableProcessHandler(commandLine)

        val settings = TexifySettings.getInstance()
        if(settings.texFlavor == TexFlavor.TexLive){
            val inputs = (moduleRoots + arrayOf(runConfig.mainFile?.parent))
                    .filter { it != null }
                    .joinToString(":") { it.path }
            commandLine.environment["BIBINPUTS"] = inputs
        }

        // Reports exit code to run output window when command is terminated
        ProcessTerminatedListener.attach(handler, environment.project)

        return handler
    }
}
