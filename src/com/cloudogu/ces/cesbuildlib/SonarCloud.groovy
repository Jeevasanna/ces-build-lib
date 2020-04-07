package com.cloudogu.ces.cesbuildlib

/**
 * Abstraction for SonarCloud. More or less a special SonarQube instance.
 *
 * The integration into GitHub, BitBucket and such is done via 3rd party integrations into those tools.
 * Normal SonarQube uses e.g. the GitHub plugin.
 */
class SonarCloud extends SonarQube {

    SonarCloud(script, Map config) {
        super(script, config)

        this.isUsingBranchPlugin = true
    }

    void initMavenForPullRequest(Maven mvn) {
        script.echo "SonarQube analyzing PullRequest ${script.env.CHANGE_ID}."

        def git = new Git(script)
        String repoUrl = git.repositoryUrl
        String repoName = git.repositoryName

        mvn.additionalArgs +=
                // an additional space is required in the case of pre existing additionalArgs
                " " +
                "-Dsonar.pullrequest.base=${script.env.CHANGE_TARGET} " +
                "-Dsonar.pullrequest.branch=${script.env.CHANGE_BRANCH} " +
                "-Dsonar.pullrequest.key=${script.env.CHANGE_ID} "

        if (repoUrl.contains('github.com')) {
            // See https://sonarcloud.io/documentation/integrations/github
            mvn.additionalArgs +=
                    "-Dsonar.pullrequest.provider=GitHub " +
                    "-Dsonar.pullrequest.github.repository=${repoName} "
        } else if (repoUrl.contains('bitbucket.org')) {
            String owner = repoName.split('/')[0]
            String plainRepoName = repoName.split('/')[1]

            mvn.additionalArgs +=
                    "-Dsonar.pullrequest.provider=bitbucketcloud " +
                    "-Dsonar.pullrequest.bitbucketcloud.owner=${owner} " +
                    "-Dsonar.pullrequest.bitbucketcloud.repository=${plainRepoName} "
        } else {
            script.error "Unknown sonar.pullrequest.provider. None matching for repo URL: ${repoUrl}"
        }
    }

    @Override
    protected void initMaven(Maven mvn) {
        if (script.isPullRequest()) {
            initMavenForPullRequest(mvn)
        } else {
            initMavenForRegularAnalysis(mvn)
        }

        if (config['sonarOrganization']) {
            mvn.additionalArgs += " -Dsonar.organization=${config['sonarOrganization']} "
        }
    }

    @Override
    protected void validateMandatoryFieldsWithoutSonarQubeEnv() {
        super.validateMandatoryFieldsWithoutSonarQubeEnv()
        // When using SonarQube env, the sonarOrganization might be set there as SONAR_EXTRA_PROPS
        validateFieldPresent(config, 'sonarOrganization')

    }
}
