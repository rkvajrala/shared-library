import groovy.json.JsonSlurper
import groovy.json.*
import groovy.json.JsonSlurperClassic

def getCodeCloudURL() {
    
        def gitPrefix = ("${WORKSPACE}".split('com.att.bwms'))[1].split('/')
        
        if ((gitPrefix.contains('CLM')) || (gitPrefix.contains('RETAIL')) || (gitPrefix.contains('CARE')) || (gitPrefix.contains('C360')) ){
        
            prefix = gitPrefix[2]
          
          }
              
        else {
                  prefix = gitPrefix[1]
                
              }
              
              //println(prefix)

              def codeCloudURL = "https://codecloud.web.att.com/scm/st_bwmsmule/" + prefix + '.git'
              
              return codeCloudURL
              
}

def getCreds() {
    withCredentials([usernamePassword(credentialsId: 'Anypoint-DevOps-Prod-ConnectedApp', usernameVariable: 'anypointUsername', passwordVariable: 'anypointPassword')]){
			  def credentails = ["anypointUsername" : anypointUsername, "anypointPassword" : anypointPassword]
			  println("== Anypoint Platform Credentials are sucessfully obtained for Prod deployment ==")
			  return credentails
      }
}
def getCredsNonProd() {
    withCredentials([usernamePassword(credentialsId: 'MyConnectedAPP', usernameVariable: 'anypointUsernameNP', passwordVariable: 'anypointPasswordNP')]){
			  def credentailsNP = ["anypointUsernameNP" : anypointUsernameNP, "anypointPasswordNP" : anypointPasswordNP]
			  println("== Anypoint Platform Credentials are sucessfully obtained for non-Prod deployment == ==")
			  return credentailsNP
      }
}

def getAnypointToken(anypointClientId, anypointSecret) {
        final def(String tokenResponse, String code) = sh(script: "#!/bin/sh -e\n \
			curl -w '\\n%{response_code}' --location --request POST 'https://anypoint.mulesoft.com/accounts/api/v2/oauth2/token' \
			--header 'Content-Type: application/x-www-form-urlencoded' \
			--data-urlencode 'client_id=${anypointClientId}' \
			--data-urlencode 'client_secret=${anypointSecret}' \
			--data-urlencode 'grant_type=client_credentials'", returnStdout: true).trim().tokenize("\n")
		println("== Anypoint token  response code:" + code);
		  if(code != '200'){
		  	throw new Exception("=== Unable to authorize with given credentials, Anypoint response --> " + tokenResponse)
		  } 
		def token = new JsonSlurper().parseText(tokenResponse)
		if(token.access_token.size() < 0){
			throw new Exception("Failed to get the login token! --> " + token)
		}
		def authorization = "Bearer " + token.access_token
		println("Authorization token sucessfully obtained")
		return authorization
}
			 
def getBGIDandEID(businessGroupName, environmentName){

		def businessGroupId = '85c0b1e4-2d12-476c-aadd-6685bae4edde'
		def environmentId = '9bc4d243-5497-4fbf-9c4d-c94fbbdadd91'
		def clientId = 'e8be06e20c6c4105b9ce69432ad54659'
		def clientSecret = 'D0836e2F03a644dFaD67919320352D70'
		println("== Environment Id for Requested Environment is " + environmentName + ":" + environmentId + " ==")
		def responseObj = ['businessGroupName': businessGroupName, 'businessGroupId' : businessGroupId, 'environmentId': environmentId, 'clientId' : clientId, 'clientSecret': clientSecret]
		println("== Business Group Id and Environment Id is sucessfully obtained for requested environment :  " + environmentName + " ==")
		return responseObj


}

def getListDeployedApps(authorization, businessGroupId, environmentId) {
				  def getArmResponse = httpRequest contentType: 'APPLICATION_JSON', httpMode: 'GET',  customHeaders: [[maskValue: true, name: 'X-ANYPNT-ENV-ID', value: environmentId], [maskValue: true, name: 'X-ANYPNT-ORG-ID', value: businessGroupId], [maskValue: false, name: 'Authorization', value: authorization]], ignoreSslErrors: true, url: 'https://anypoint.mulesoft.com/armui/api/v1/applications', wrapAsMultipart: false
		          def parsedResponse = new JsonSlurper().parseText(getArmResponse.content)
				  if(parsedResponse.size() > 0)
				   {
				   	 println("== Sucessfully obtained the list of deployed applications from requested environment : " + environmentId + " ==")

					}
				   else
					{
						throw new Exception("Failed to get the Deployed apps! --> " + getArmResponse)
					}
				   
				  return parsedResponse
		         

}

def deployApplication(authorization, businessGroupId, environmentId, appInfoJson, file, appName, deployMode, environmentName){

	if(deployMode == "redeploy") {
	
		println("== Application found. will be redeployed ==")
    	deploy = sh(script: "#!/bin/sh -e\n \
    	                        curl --location --request PUT 'https://anypoint.mulesoft.com/cloudhub/api/v2/applications/${appName}' \
								--proxy 'http://pxyapp.proxy.att.com:8080' \
            			        --header 'Authorization:    ${authorization}' \
            			        --header 'X-ANYPNT-ENV-ID: ${environmentId}' \
            			        --header 'X-ANYPNT-ORG-ID: ${businessGroupId}' \
            			        --header 'Content-Type: multipart/form-data' \
            			        --header 'Accept: application/json' \
            			        --form 'appInfoJson=${appInfoJson}' \
            			        --form 'autoStart=true' \
            			        --form 'file=@${WORKSPACE}/target/${file}'", returnStdout: true)
    

	}
	
	else {
	
		println("== Application Notfound, new application will be deployed ==")
	
    	deploy = sh(script: "#!/bin/sh -e\n \
    	                        curl  --location --request POST 'https://anypoint.mulesoft.com/cloudhub/api/v2/applications' \
								--proxy 'http://pxyapp.proxy.att.com:8080' \
            			        --header 'Authorization:    ${authorization}' \
            			        --header 'X-ANYPNT-ENV-ID: ${environmentId}' \
            			        --header 'X-ANYPNT-ORG-ID: ${businessGroupId}' \
            			        --header 'Content-Type: multipart/form-data' \
            			        --header 'Accept: application/json' \
            			        --form 'appInfoJson=${appInfoJson}' \
            			        --form 'autoStart=true' \
            			        --form 'file=@${WORKSPACE}/target/${file}'", returnStdout: true)
    
  	
    }
	
	def deployResponse = new JsonSlurper().parseText(deploy)
	
	if (deployResponse.keySet().contains("versionId")) {
	
		println("== Application Deployed Sucessfully in requested environment : " + environmentName + " ==")
	}
	
	else {
		
		println("== Application Deployment Failed in requested environment : " + environmentName + " ==")
		throw new Exception("Application Deployment Failed ===> " + deploy)
	
	}
    
    
}

def updateProperties(authorization, businessGroupId, environmentId, appInfoJson, file, appName, environmentName){
	println("== Properties will be updated in ARM ==")
	deploy = sh(script: "#!/bin/sh -e\n \
							curl --location --request PUT 'https://anypoint.mulesoft.com/cloudhub/api/v2/applications/${appName}' \
							--proxy 'http://pxyapp.proxy.att.com:8080' \
							--header 'Authorization:    ${authorization}' \
							--header 'X-ANYPNT-ENV-ID: ${environmentId}' \
							--header 'X-ANYPNT-ORG-ID: ${businessGroupId}' \
							--header 'Content-Type: multipart/form-data' \
							--header 'Accept: application/json' \
							--form 'appInfoJson=${appInfoJson}' \
							--form 'autoStart=true'", returnStdout: true)
							
	def deployResponse = new JsonSlurper().parseText(deploy)
	if (deployResponse.keySet().contains("versionId")) {
		println("== Application updated properties Sucessfully in requested environment : " + environmentName + " ==")
	}
	else {
		println("== ARM application properties update Failed in requested environment : " + environmentName + " ==")
		throw new Exception("Application Deployment Failed ===> " + deploy)
	}
}

def sonarScan(deployProperties, deploymentName){
	if(deployProperties){
		sourceDeployemntName = deployProperties.deployNamePrefix + "-" + deployProperties.targetEnvironmentName.toLowerCase() + "-" + deploymentName
	}else{
		sourceDeployemntName = deploymentName
	}
	script{
		sonarProperties = """
		sonar.host.url=https://sonar.it.att.com
		sonar.att.motsid=30686
		sonar.login=4d7132798b9098bc87a6fa24b935d0598a13f026
		sonar.password=
		sonar.mule.file.suffixes=.xml 
		sonar.xml.file.suffixes=.wsdl
		sonar.att.view.type=dev
		
		sonar.att.tattletale.enabled=false
		sonar.projectName=STBWMSMULE:30686:${sourceDeployemntName}
		sonar.projectVersion=1.0
		sonar.projectKey=STBWMSMULE:30686:${sourceDeployemntName}
		sonar.sources=src/main/mule
		"""
		sonarProperties.stripMargin()
		println("=== sonarProperties ===" + sonarProperties)
		writeFile file: 'sonar.properties', text: "${sonarProperties}"
		sonar()
	}
}

def staticCodeAnalysis(rulesVersion){
	staticCodeRulesVersion = readMavenPom().getProperties().getProperty('staticCodeRulesVersion')
	if(staticCodeRulesVersion == null){
		println("== staticCodeRulesVersion is not found on pom.xml default vesion is going select, default version is : " + rulesVersion + " ==")
		staticCodeRulesVersion = rulesVersion
	}
	else{
		println("== staticCodeRulesVersion found on pom.xml is : " + staticCodeRulesVersion + " ==")
	}
	rulesFileName = "MuleValidationRules-" + staticCodeRulesVersion + ".txt"
	println("== Rule file name is : " + rulesFileName + " ==")
	sh 'rm mule-validation -rf; mkdir mule-validation'
	println("== Sucessfully Created mule-validation sub directory ==")
	dir('mule-validation'){
		git branch: 'master',
		credentialsId: 'MuleMechId',
		url: 'https://codecloud.web.att.com/scm/st_bwmsmule/mule-static-code-analysis-rules.git'
		println("== Sucessfully cloned static code rules in to mule-validation sub directory ==")
		withMaven(jdk: 'jdk8', maven: 'maven', mavenSettingsConfig: 'maven-central', publisherStrategy: 'EXPLICIT', tempBinDir: ''){
			sh """
				 mvn -Dmaven.repo.local=$WORKSPACE/mule-validation \
					  org.apache.maven.plugins:maven-dependency-plugin:2.8:get \
					  -DgroupId=com.att.bwms \
					  -DartifactId=muleCodeChecker \
					  -Dversion=1.0.0-SNAPSHOT \
					  -Dpackaging="jar" \
					  -DremoteRepositories="http://mavencentral.it.att.com:8081/nexus/content/repositories/att-repository-snapshots"
				"""
			println("== Sucessfully downloaded muleCodeChecker utility jar into mule-rules sub directory from Maven Central ==")
			staticCode = sh( script : "java -jar $WORKSPACE/mule-validation/com/att/bwms/muleCodeChecker/1.0.0-SNAPSHOT/muleCodeChecker-1.0.0-SNAPSHOT.jar  $WORKSPACE default $WORKSPACE/mule-validation/${rulesFileName} RAW", returnStdout: true)
			def response = new JsonSlurper().parseText(staticCode)
			if (response == false){
				println("== Static code analysis sucessfully completed. Final result of the analysis is failed, please check the logs for detailed information ==")
				println(staticCode)
				throw new Exception("static code analysis failed")
			}
			println("== Static code analysis sucessfully completed. Application code is sucessfully validated ==")
		}
	}
	sh"rm mule-validation -rf; rm mule-validation@tmp -rf"
	println("== Sucessfully removed mule-validation sub directory from WROKSPACE ==")				
}

def notifyBuild(String buildStatus = 'STARTED') {
	// build status of null means successful
	buildStatus =  buildStatus ?: 'SUCCESSFUL'
 
	def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
	def summary = "${subject} (${env.BUILD_URL})"
    def details = """STATUS:  ${buildStatus}\n\n Branch: ${envVariables.codeCloudBranch}\n Job: '${env.JOB_NAME} [${env.BUILD_NUMBER}]'\n Check console output at "${env.BUILD_URL}"\n Job Name: ${env.JOB_NAME}\n Build Number:  [${env.BUILD_NUMBER}]\n\n Regards,\n MuleSoft DevOps Team"""
   
	mail subject: subject,
	body: details,
	to: notificationEmailId,
	from: "noreply@att.com" 

  println("== ERROR IN BUILD - Email Notification Sent ==")
}

def getProperties(Map value, List<String> prefix = []) {
  value.collectEntries {k, v ->
    if( v == null){
      println("====Validate YAML Config file===")
      throw new Exception("====Validate YAML Config file===")
    }
    getProperties(v, prefix + k)
  }
}
            
def getProperties(List value, List<String> prefix = []) {
              value.indexed().collectEntries { i, v ->
                  getProperties(v, prefix + i)
              }
            }
            
def getProperties(Object value, List<String> prefix = []) {
            return  [(prefix.join('.')): value]
            }


 
