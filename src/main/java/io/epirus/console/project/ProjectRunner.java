/*
 * Copyright 2020 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.epirus.console.project;

import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi;

public abstract class ProjectRunner implements Runnable {

    public String projectName;
    public String packageName;
    public String outputDir;

    public ProjectRunner(final ProjectCreatorConfig projectCreatorConfig) {
        this.projectName = projectCreatorConfig.getProjectName();
        this.packageName = projectCreatorConfig.getPackageName();
        this.outputDir = projectCreatorConfig.getOutputDir();
    }

    @Override
    public void run() {
        createProject();
    }

    protected abstract void createProject();

    protected void onSuccess(Project project, String projectType) {
        String gradleCommand =
                System.getProperty("os.name").toLowerCase().startsWith("windows")
                        ? "./gradlew.bat"
                        : "./gradlew";
        System.out.print(System.lineSeparator());
        ColoredPrinter cp =
                new ColoredPrinter.Builder(0, false)
                        .foreground(Ansi.FColor.WHITE)
                        .background(Ansi.BColor.GREEN)
                        .attribute(Ansi.Attribute.BOLD)
                        .build();
        ColoredPrinter instructionPrinter =
                new ColoredPrinter.Builder(0, false).foreground(Ansi.FColor.CYAN).build();
        ColoredPrinter commandPrinter =
                new ColoredPrinter.Builder(0, false).foreground(Ansi.FColor.GREEN).build();
        cp.println("Project Created Successfully");
        System.out.print(System.lineSeparator());

        if (project.getProjectWallet() != null) {
            instructionPrinter.println(
                    "Project information",
                    Ansi.Attribute.LIGHT,
                    Ansi.FColor.WHITE,
                    Ansi.BColor.BLACK);
            instructionPrinter.print(
                    String.format("%-20s", "Wallet Address"),
                    Ansi.Attribute.CLEAR,
                    Ansi.FColor.WHITE,
                    Ansi.BColor.BLACK);
            instructionPrinter.println(
                    project.getProjectWallet().getWalletAddress(),
                    Ansi.Attribute.BOLD,
                    Ansi.FColor.GREEN,
                    Ansi.BColor.BLACK);
            System.out.print(System.lineSeparator());
        }
        instructionPrinter.println(
                "Commands", Ansi.Attribute.LIGHT, Ansi.FColor.YELLOW, Ansi.BColor.BLACK);
        instructionPrinter.print(String.format("%-40s", gradleCommand + " test"));
        commandPrinter.println("Test your application");
        instructionPrinter.print(String.format("%-40s", "epirus run rinkeby|ropsten"));
        commandPrinter.println("Runs your application");
        instructionPrinter.print(String.format("%-40s", "epirus docker run rinkeby|ropsten"));
        commandPrinter.println("Runs your application in a docker container");
    }
}
