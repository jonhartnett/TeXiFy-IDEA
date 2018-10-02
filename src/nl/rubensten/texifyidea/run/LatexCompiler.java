package nl.rubensten.texifyidea.run;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import nl.rubensten.texifyidea.settings.TexFlavor;
import nl.rubensten.texifyidea.settings.TexifySettings;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ruben Schellekens, Sten Wessel
 */
public enum LatexCompiler {

    PDFLATEX("pdfLaTeX", "pdflatex"), LATEXMK("LaTeXMk", "latexmk");

    private String displayName;
    private String executableName;

    LatexCompiler(String displayName, String executableName) {
        this.displayName = displayName;
        this.executableName = executableName;
    }

    @Nullable
    public List<String> getCommand(LatexRunConfiguration runConfig, Project project) {
        List<String> command = new ArrayList<>();

        ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
        ProjectFileIndex fileIndex = rootManager.getFileIndex();
        VirtualFile mainFile = runConfig.getMainFile();
        VirtualFile moduleRoot = fileIndex.getContentRootForFile(runConfig.getMainFile());
        VirtualFile[] moduleRoots = rootManager.getContentSourceRoots();
        if (moduleRoot == null || mainFile == null) {
            return null;
        }

        if (this == PDFLATEX || this == LATEXMK) {
            if (runConfig.getCompilerPath() != null) {
                command.add(runConfig.getCompilerPath());
            } else {
                command.add(executableName);
            }
            command.add("-file-line-error");
            command.add("-interaction=nonstopmode");
            command.add("-synctex=1");
            if(this == PDFLATEX)
                command.add("-output-format=" + runConfig.getOutputFormat().name().toLowerCase());
            else
                command.add("-" + runConfig.getOutputFormat().name().toLowerCase());
            command.add("-output-directory=" + moduleRoot.getPath() + "/out");

            TexifySettings settings = TexifySettings.getInstance();

            if(settings.getTexFlavor() == TexFlavor.TexLive && this == LATEXMK){
                StringBuilder inputs = new StringBuilder();
                for(VirtualFile root : moduleRoots)
                    inputs.append(root.getPath()).append(":");

                command.add("-e");
                command.add("$ENV{'TEXINPUTS'} = '" + inputs + "' . $ENV{'TEXINPUTS'}");
            }

            if(settings.getTexFlavor() == TexFlavor.MikTex){
                if (runConfig.hasAuxiliaryDirectories()) {
                    command.add("-aux-directory=" + moduleRoot.getPath() + "/auxil");
                }

                for (VirtualFile root : moduleRoots) {
                    command.add("-include-directory=" + root.getPath());
                }
            }
        }

        // Custom compiler arguments specified by the user
        if (runConfig.getCompilerArguments() != null) {
            String[] args = runConfig.getCompilerArguments().split("\\s+");
            command.addAll(Arrays.asList(args));
        }

        command.add(mainFile.getName());

        return command;
    }

    public String getExecutableName() {
        return executableName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

    /**
     * @author Ruben Schellekens
     */
    public enum Format {

        PDF,
        DVI;

        @Nullable
        public static Format byNameIgnoreCase(@Nullable String name) {
            for (Format format : values()) {
                if (format.name().equalsIgnoreCase(name)) {
                    return format;
                }
            }

            return null;
        }
    }
}
