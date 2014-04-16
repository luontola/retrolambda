package net.orfjackal.retrolambda.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "process", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class RetrolambdaMojo extends AbstractMojo {

	@Component
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Component
	private MavenProject project;

	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;

	public void execute() throws MojoExecutionException {
		executeMojo(
				plugin(groupId("org.apache.maven.plugins"),
						artifactId("maven-dependency-plugin"), version("2.0")),
				goal("copy"),
				configuration(element(
						"artifactItems",
						element("artifactItem",
								element(name("groupId"),
										"net.orfjackal.retrolambda"),
								element(name("artifactId"), "retrolambda"),
								element(name("version"), "1.1.4"),
								element(name("overwrite"), "true"),
								element(name("outputDirectory"), project
										.getBuild().getOutputDirectory()),
								element(name("destFileName"), "retrolambda.jar")))),
				executionEnvironment(project, session, pluginManager));
	}
}
