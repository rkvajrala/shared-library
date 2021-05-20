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
			def appProperties = commonUtils.getProperties(parsedJson)
						
			//sh'''
			//				rm -r $WORKSPACE/external-properties
			//'''''

			sh "echo workspace : ${WORKSPACE}/${configPath}"
			sh "echo appProperties : ${appProperties}"
			
			println("== Sucessfully Read the properties from location external-properties/config-" + deployProperties.targetEnvironmentName + ".yaml ==")
				
		}
		stage("Maven Package and MUnit testing") 
		{
         
          	def gitTag = sh(returnStdout: true, script: "git tag --contains | head -1").trim()
         
            def gitBranch = envVariables.cloneBranch
         
            def builtOn = new Date().format("yyyy-MM-dd.HH:mm:ss", TimeZone.getTimeZone('UTC'))
         
            def builtBy = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%an'").trim()
         
            def buildInfo = JsonOutput.toJson([buildInformation:[buildNumber: "${BUILD_NUMBER}", gitTag: gitTag ,gitBranch: gitBranch, builtOn: builtOn, builtBy: builtBy]])
         
            println("== buildInfo == " + buildInfo)
    

           	writeJSON file: 'src/main/resources/META-INF/build-metadata/buildInfo.json', json: buildInfo
	
		 	withMaven(jdk: 'jdk8', maven: 'maven', mavenSettingsConfig: 'MAVEN_SETTINGS') {
		 		sh '''echo
		 		pwd
		 		ls -l
		 		which mvn
		 		mvn clean package -Dbuild.number=HF${BUILD_NUMBER} -Dmule.env=dev -Dmule.key=$Mule_KEY -DskipMunitTests=true
		 		'''
		 		println("== Maven Package Build sucessfully completed without MUnit for hotfix==")
		 	}
		}
		stage("Obtain Anypoint Token") {
						creds = commonUtils.getCredsNonProd() // anypointUsername and anypointPassword
						println("Credential Used For Anypoint Platform: " + creds.anypointUsernameNP)
						//authorization = commonUtils.getAnypointToken(creds.anypointUsernameNP, creds.anypointPasswordNP)

				
				}

	}
	

}
