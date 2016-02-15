# AWS Cognito Sync Issues

This project exhibits the issues with using Amazon Cognito Sync.
Those issues are around the Cognito credentials refresh method and strategies.

Once identified, Cognito issues temporary credentials (expire after 1 hour) that allow you to perform dataset synchronisation with Cognito Sync.

We faced 1 issue:
* Once Cognito Credentials have expired, they cannot be refreshed any more (we have not found how at least) and any sync attempt leads to a `com.amazonaws.services.cognitoidentity.model.NotAuthorizedException: Access to Identity 'eu-west-1:xxxx' is forbidden. (Service: AmazonCognitoIdentity; Status Code: 400; Error Code: NotAuthorizedException; Request ID: zzzz)`

What is really disturbing is that we have as well highlighted that Cognito can refresh the credentials automatically in some situations (see below)

## What does it do?
In order to highlight the issues above, we have built this small application:
* it allows signing in with Facebook and/or Google
* it shows the Cognito session expiration
* one button will put data ("when" and "phone") into a Cognito dataset and will synchronize it to the remote storage
* another button will refresh the datasets (pulling from the remote storage)
* it can be used with the same user logged on 2 different devices

## How to build/run it?
You will need to have at least a Google signin service or a Facebook app to allow signin with either or both.
* Follow the steps in the [AWS Cognito documentation](https://docs.aws.amazon.com/cognito/devguide/identity/identity-pools/) to create you identity pool.
* Put your identity pool in the [resource file configuration_strings.xml](app/src/main/res/values/configuration_strings.xml)
* If you want to use Facebook login, create your Facebook app and [declare it as an identity provider](https://docs.aws.amazon.com/cognito/devguide/identity/external-providers/facebook/)
* Put your Facebook App ID in configuration_strings.xml
* If you want to use Google Signin [create & configure a project in the Google Developer Console and declare it as an OpenID provider in your AWS console](https://docs.aws.amazon.com/cognito/devguide/identity/external-providers/google/).
* Put your Google Service Client ID in configuration_strings.xml. *Beware* this is the ID of the Web service that you need to use.
* Download the google_service.json and put it directly in the [app](app) directory.

You now should be able to build the application.

## How is it architectured?
The application is over simplified, but yet we kept an architecture that fits a full blown application (rather than the "Hellow world" kind).

* There are 2 activities
  * MainActivity shows the credentials expiration date, the values in the dataset and boast 2 buttons to put values and retrieve values from the remote.
  * SignInActivity is started by MainActivity when no identity is present on the device
* All the interactions with Cognito have been implemented in the AccountManager singleton.
  * It gets the new identity when the user logs in
  * It exposes the data stored in the dataset and session expiration
  * It exposes a method to put new data in the dataset (this methods triggers a sync and tries to refresh the Cognito credentials if necessary).

## How to trigger the issue?

### Credential cannot be refreshed while expired
* Clear application data if you used it once
* Launch the application
* Signin with your preferred method
* The credential expiration should display one hour from now
* _Play_ with the application a bit (put data, refresh data)
* After one hour (after the credential have expired), the application should *crash* if you press either "Put data" or "Refresh data". The console should show something along the lines of `com.amazonaws.services.cognitoidentity.model.NotAuthorizedException: Access to Identity 'eu-west-1:xxxx' is forbidden. (Service: AmazonCognitoIdentity; Status Code: 400; Error Code: NotAuthorizedException; Request ID: zzzz)`

### ...While they are perfectly refreshed if a sync was requested while a network blackout
* Clear application data if you used it once
* Launch the application
* Signin with your preferred method
* _Play_ with the application a bit (put data, refresh data)
* At _expiration_ **minus 5 minutes**, shutdown your devices 3G & wifi (any network really)
* Still before expiration, _play_ with the application once more (put data, it cannot be synchronized, but as we called `synchronizeOnConnectivity`, it should be synchronized as soon as connectivity comes back)
* Wait till _expiration_ **plus 2 minutes**
* Reenable Wifi (or 3G, or any network)
* You should see the extended session expiration in the UI (and logs) sign that the SDK automatically detected that the session was expired and renewed the session.

