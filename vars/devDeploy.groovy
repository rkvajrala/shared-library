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

			runtimeVersion = readMavenPom().getProperties().getProperty('app.runtime')
			
			dPrefix = readMavenPom().getProperties().getProperty('deployment.prefix')
			dSuffix = readMavenPom().getProperties().getProperty('deployment.suffix')

						
			sh "echo groupId : ${groupId}"	

			println("== Sucessfully clone the source code in to jenkins workspace from codeCloud ==") 		
						

		}
		stage('Get Application Properties') 
		{
			def configPath = 'src/main/resources/' + deployProperties.targetEnvironmentName +'.yaml'
			def yamlProps = readYaml file: "${WORKSPACE}/${configPath}"
			def jsonProps = JsonOutput.toJson(yamlProps)
			def parsedJson = new JsonSlurperClassic().parseText(jsonProps)
			//appProperties = commonUtils.getProperties(parsedJson)
						
			//sh'''
			//				rm -r $WORKSPACE/external-properties
			//'''''

			sh "echo workspace : ${WORKSPACE}/${configPath}"
			sh "echo properties filePath : ${parsedJson}"
			
			println("== Sucessfully Read the properties from location external-properties/config-" + deployProperties.targetEnvironmentName + ".yaml ==")
				
		}

	}
	

}
