# sms2sns Android Application

This is a simple Android app that receives SMS (text) messages and forwards them to an Amazon AWS SNS queue.
This is useful for testing receipt of SMS messages in Amazon AWS while waiting for a long code to be assigned, something that
can take several business days.  

## Configuring AWS

This app assumes you already have an Amazon SNS topic set up.  If you do not, you will need
to create one for this app to do anything useful.  If you do, you'll need to go into the AWS
console and get the ARN of that queue.  You'll need it for the next steps.

You must complete the two steps 

### STEP 1: Creating a Lambda

In order to insert messages into your SNS queue, you need to set up a Lambda to process
the requests.

1. Log in to https://console.aws.amazon.com
2. Tap Compute -> Lambda -> Create Function
3. Select “Author from scratch” then enter the following values:
   Name: SnsForwarderLambda
   Runtime: Node.js 6.10
   Role: Create new role from template(s)
   Role Name: SnsForwarderRole
4. Under Policy Templates, choose "Simple Microservice permissions" and  "SNS publish policy"
5. Tap "Create function"
6. Once the Lambda is created, you can paste in the code below.  You will need to replace the string ====PASTE_YOUR_SNS_TOPIC_ARN_HERE==== with the ARN of your SNS topic.  It should look something like this: arn:aws:sns:us-east-1:012345678901:mytopicname

 ```
 console.log("Loading function");
 var AWS = require("aws-sdk"); 
 
 exports.handler = function(event, context, callback) {
     console.log("Received event:", JSON.stringify(event));
     var messageText = JSON.parse(event.body).sns_message;
     console.log("Message text is:", messageText); 



     var sns = new AWS.SNS();
     var params = {
         Message: messageText, 
         Subject: null,
         TopicArn: "====PASTE_YOUR_SNS_TOPIC_ARN_HERE===="
     };
     sns.publish(params, context.done);
     const response = { statusCode: 200, body: JSON.stringify({"status": "ok"}) };
     callback(null, response);  
     console.log("Send callback");
     context.done;
 };
 ```
7. Hit Save


### STEP 2: Making an API gateway

1. Go to https://console.aws.amazon.com/
2. Select Networking and Content Delivery -> API Gateway
3. Choose to Create a New API.  
4. On the API creation screen fill out the following fields:
  Type: New API
  API Name: SNSForwarderAPI
  Description: (leave blank)
  Endpoint Type: Regional
5. Tap “Create API”
6. You will see an API editor screen.  Under the “Actions” pull down menu, choose “Create” Method, then in the picklist choose “POST”. 
7. Update the following fields:
   Integration Type: Lambda
   Lambda: SnsForwarderLambda
   Lambda Proxy Integration: CHECKED
8. Tap “Save”
9. Using the “Actions” pull-down menu, select Deploy.  In the dialog that pops up, enter:
   
  Deployment stage: [New Stage]
  Stage name: test
  Stage description; (leave blank)
  Deployment description (leave blank)
   
10. Tap Deploy
11. Wait for the spinner to complete.  When done, you’ll see a new stage has been created, and the URL for your gateway resource will be available.  It should give you an invoke URL that looks something like this: https://abcdeghijk.execute-api.us-east-1.amazonaws.com/test

#### STEP 3: Testing the API

Run the following from the command line to make sure the API you set up works.  You'll need to replace the URL with the URL from the previous step.

```
$ curl -X PUT https://abcdeghijk.execute-api.us-east-1.amazonaws.com/test -d '{"sns_message":"my message"}'

{"status":"ok"}
```

If you see the status: ok response, it is working.


## Compiling and Configuring this App

Before this app will compile, you must insert values for two constants in the SmsBroadcastReceiver.java class:

```
    private static final String AMAZON_URL = "https://abcd1234.execute-api.us-east-1.amazonaws.com/test";
    private static final String MESSAGE_PREFIX_FOR_FORWARDING = "device_uuid:"; 
```

The first value above is the URL of your gateway obtained in STEP 2 of the configuration section.

The second value is the prefix that each SMS message must start with in order for it to be forwarded to SNS.  Edit this as is needed for your use case.

Once you modify the above, simply open the outermost `build.gradle`` file in Android Studio 3+, connect your Android device via a USB cable and run it.


## License

```
Copyright 2018 David G. Young

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
