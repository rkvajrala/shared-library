def call(Map config= [:])
{ 
	
	envVariables = [
						"cloneURL" : ${config.cURL},
						"cloneBranch" : ${config.cBranch},
      					"email" : ${config.email}
					]
	/*
	deployProperties = [

					"businessGroupName" : ${config.bgName},
					"targetEnvironmentName" : ${config.teName},
					"numberOfWorkers" : ${config.nofWorkers},
					"workerSize" : ${config.wSize},
					"deployRegion" : ${config.dRegion},
					"deployNamePrefix" : ${config.dnPrefix},
					"muleKeyId" : ${config.mkId}
				]

  */
	sh "echo envVariables and config variables initialized!"
}
