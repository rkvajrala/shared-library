import groovy.json.JsonSlurper
import groovy.json.*
import groovy.json.JsonSlurperClassic


def call(Map config= [:])
{ 
	
	sh "echo Start setting env and deployment config variables"

	
	envVariables = [
	 					"cloneURL" : config.cURL,
	 					"cloneBranch" : config.cBranch,
       					"email" : config.email
	 				]
	 sh "echo envVariables : ${envVariables}"
	deployProperties = [

					"businessGroupName" : config.bgName,
					"targetEnvironmentName" : config.teName,
					"numberOfWorkers" : config.nofWorkers,
					"workerSize" : config.wSize,
					"deployRegion" : config.dRegion,
					"deployNamePrefix" : config.dnPrefix,
					"muleKeyId" : config.mkId
				]

  	sh "echo deployVariables : ${deployProperties}"
	sh "echo End of setting env and deployment config variables"

	sh "echo cloneURL : ${envVariables.cloneURL}"

	rulesVersion = "1.0.0"


	node
	{

		stage('CloneRepo')
		{
			git branch: envVariables.cloneBranch,
				url: envVariables.cloneURL

			groupId = readMavenPom().getGroupId()
			artifactId = readMavenPom().getArtifactId()
			version = readMavenPom().getVersion()
			sh "echo groupId : ${groupId}"			
						

		}
		stage('Stage2')
		{
			sh 'echo stage2'

		}

	}
	

}
