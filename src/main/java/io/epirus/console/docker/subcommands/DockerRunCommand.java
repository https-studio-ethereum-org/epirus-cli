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
package io.epirus.console.docker.subcommands;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import io.epirus.console.EpirusVersionProvider;
import io.epirus.console.docker.DockerOperations;
import io.epirus.console.project.InteractiveOptions;
import io.epirus.console.wrapper.CredentialsOptions;
import org.apache.commons.lang3.ArrayUtils;
import picocli.CommandLine;

import org.web3j.codegen.Console;

import static io.epirus.console.EnvironmentVariablesProperties.*;
import static io.epirus.console.config.ConfigManager.config;
import static picocli.CommandLine.Help.Visibility.ALWAYS;

@CommandLine.Command(
        name = "run",
        description = "Run project in docker",
        showDefaultValues = true,
        abbreviateSynopsis = true,
        mixinStandardHelpOptions = true,
        versionProvider = EpirusVersionProvider.class,
        synopsisHeading = "%n",
        descriptionHeading = "%nDescription:%n%n",
        optionListHeading = "%nOptions:%n",
        footerHeading = "%n",
        footer = "Epirus CLI is licensed under the Apache License 2.0")
public class DockerRunCommand implements DockerOperations, Runnable {

    @CommandLine.Option(names = {"-t", "--tag"})
    String tag = "web3app";

    @CommandLine.Parameters(
            index = "0",
            paramLabel = "network",
            description = "Ethereum network [rinkeby/kovan]",
            arity = "1")
    String deployNetwork;

    @CommandLine.Option(names = {"-l", "--local"})
    boolean localMode;

    @CommandLine.Mixin CredentialsOptions credentialsOptions;

    @CommandLine.Option(
            names = {"-d", "--directory"},
            description = "Directory to run docker in.",
            showDefaultValue = ALWAYS)
    Path directory = Paths.get(System.getProperty("user.dir"));

    @CommandLine.Option(names = {"-p", "--print"})
    boolean print;

    @Override
    public void run() {

        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd().withShowAll(true);

        if (listImagesCmd.exec().stream()
                .flatMap(i -> Arrays.stream(i.getRepoTags()))
                .noneMatch(j -> j.startsWith(tag))) {
            if (new InteractiveOptions()
                    .userAnsweredYes(
                            "It seems that no Docker container has yet been built. Would you like to build a Dockerized version of your app now?")) {
                try {
                    executeDocker(
                            new String[] {"docker", "build", "-t", tag, "."},
                            Paths.get(System.getProperty("user.dir")).toAbsolutePath());
                } catch (Exception e) {
                    Console.exitError(e);
                }
            }
        }

        String[] args =
                new String[] {
                    "docker",
                    "run",
                    "--env",
                    String.format(EPIRUS_VAR_PREFIX + "LOGIN_TOKEN=%s", config.getLoginToken())
                };

        args = setCredentials(args);
        args = setOpenAPIEnvironment(args);

        if (localMode) {
            args =
                    ArrayUtils.addAll(
                            args,
                            "-v",
                            String.format(
                                    "%s/.epirus:/root/.epirus", System.getProperty("user.home")));
        }

        args = ArrayUtils.addAll(args, tag);

        if (print) {
            System.out.println(String.join(" ", args));
            return;
        }

        try {
            executeDocker(args, directory);
        } catch (Exception e) {
            Console.exitError(e);
        }
    }

    private String[] setOpenAPIEnvironment(final String[] args) {
        return ArrayUtils.addAll(
                args,
                "--env",
                String.format(WEB3J_OPENAPI_VAR_PREFIX + "HOST=%s", "0.0.0.0"),
                "--env",
                String.format(WEB3J_VAR_PREFIX + "NETWORK=%s", deployNetwork),
                "--env",
                String.format(WEB3J_OPENAPI_VAR_PREFIX + "PORT=%d", 9090),
                "-p",
                9090 + ":" + 9090);
    }

    private String[] setCredentials(final String[] args) {
        if (credentialsOptions.getWalletPath() != null) {
            return getWalletEnvironment(
                    args,
                    credentialsOptions.getWalletPath(),
                    credentialsOptions.getWalletPassword());
        } else if (!credentialsOptions.getRawKey().isEmpty()) {
            return ArrayUtils.addAll(
                    args,
                    "--env",
                    String.format(
                            WEB3J_VAR_PREFIX + "PRIVATE_KEY=%s", credentialsOptions.getRawKey()));
        } else if (!credentialsOptions.getJson().isEmpty()) {
            return ArrayUtils.addAll(
                    args,
                    "--env",
                    String.format(
                            WEB3J_VAR_PREFIX + "WALLET_JSON=%s", credentialsOptions.getJson()));
        }
        return getWalletEnvironment(
                args, Paths.get(config.getDefaultWalletPath()), config.getDefaultWalletPassword());
    }

    private String[] getWalletEnvironment(
            final String[] args, final Path walletPath, final String walletPassword) {
        final List<String> strings = Arrays.asList(args);
        final String[] walletArgs =
                ArrayUtils.addAll(
                        args,
                        "--env",
                        String.format(
                                WEB3J_VAR_PREFIX + "WALLET_PATH=%s",
                                "/root/key/" + walletPath.getFileName().toString()),
                        "-v",
                        walletPath.getParent().toAbsolutePath().toString() + ":/root/key");

        if (!walletPassword.isEmpty()) {
            return ArrayUtils.addAll(
                    walletArgs,
                    "--env",
                    String.format(
                            WEB3J_VAR_PREFIX + "WALLET_PASSWORD=%s",
                            credentialsOptions.getWalletPassword()));
        }
        return strings.toArray(new String[] {});
    }
}
