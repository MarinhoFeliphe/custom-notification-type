# Custom Notification Type - WhatsApp

## Mandatory (Set up Localdev env):

[Tool for performing Liferay Client Extension related operations from the command line](https://github.com/liferay/liferay-cli/blob/main/README.md#manuall-installation-on-linux-using-curl)

[Getting Started with new Client Extensions Dev Experience aka Localdev](https://github.com/liferay/liferay-cli/blob/main/docs/GETTING_STARTED.markdown#bring-up-localdev-environment)

The last one you just need to follow until the step Bring up Localdev environment


## What is needed to create and use a custom notification type:

1. Creating a notification type from client side
  - Creating Spring Boot project template
  - Apply notification type partial
  - Make sure that everything was registered correctly

2. Creating a notification template

3. Apply the notification template to use

4. How liferay-portal and my custom notification type interact to each other

## Creating a notification type from client side

### Creating Spring Boot project template:

Here we’re creating a Spring Boot project because the BPM team just provided custom notification types in Java for this kind of project.

1. In your workspace execute the command:

	liferay ext create

2. Choose the following options: Create project from template > Name > service-springboot, define the name of the project, project’s path and so on.

### Apply notification type partial

In this section we’re going to generate more code, after this step some files will be created to help you as a base to implement your logic to send your desired notification.

Follow these steps

After that, these files inside this folder will be generated, here it’s the file that you need to modify to implement the logic to send the desired notification. 

The liferay-portal will reach your implementation through the POST request (The resourcePath that you’ve defined in the wizard will be the URI), but how does it work?

### Make sure that your custom notification type was properly registered

To make sure that your custom notification type is able to be used, you need to make sure that an OSGI component was registered for your notification type, go to Control Panel > System > Gogo Shell and type the command below:

	services "(objectClass=com.liferay.notification.type.NotificationType)"

You must be able to see 3 records, two for our OOTB notification types (Email and UserNotification) and the last one must be your custom notification type.

Here you can get the notification type reference, in this case it’s function#whatsapp but in your case it will be function + # + the id defined in these steps you will use to create your notification template in the next session.

## Creating a notification template

The notification template needs to be created from the headless side because the notification module doesn’t have a UI for custom notification templates yet.

Access the api docs https://dxp.lfr.dev/o/api > REST Applications > notification/v1.0 and go to NotificationTemplate section:

This is an example of body that you can use for create your notification template:

````
{
   "body":{
      "en_US":"Foo message"
   },
   "name":"WhatsApp Notification Template",
   "recipients": [
      {
        "phoneNumber": "__customerPhoneNumber__"
      }
    ],
   "editorType": "richText",
   "type":"function#whatsapp"
}
````

Note that in the type property you must define the same as in notification.type in the last section.

````
curl -X 'POST' \
  'https://dxp.lfr.dev/o/notification/v1.0/notification-templates' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'x-csrf-token: y555qQVa' \
  -d '{
   "body":{
      "en_US":"Foo Message"
   },
   "name":"WhatsApp Notification Template",
   "recipients": [
      {
        "phoneNumber": "__customerPhoneNumber__"
      }
    ],
   "editorType": "richText",
   "type":"function#whatsapp"}'
````

## Apply the notification template to use

Currently only the Object component uses the new Notification module, so it will be described here as an example.

Once your notification template is created you can use it in objects 

### How liferay-portal and custom notification types interact to each other

From liferay portal side we have the component FunctionNotificationType that is responsible for perform a POST request when the sendNotification method is called it should be called by other components like Objects for example passing a NotificationContext that will contains the notification template, user who triggered the notification, company and termValues (a key-map structure that you store data of the entry) all of these data will be pass as body in the POST request,

The FunctionNotificationType is built by its factory FunctionNotificationTypeFactory that you build a new instance of FunctionNotificationType for each new custom notification type.

After the sendNotification method is invoked it’ll be intercepted by:

````
@PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE,
      value = "/whatsapp/send")
  public ResponseEntity<String> create(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody String json)
    throws JsonMappingException, JsonProcessingException {

    Twilio.init(_twilioAccountSID, _twilioAuthToken);

    Message message = Message.creator(
            new PhoneNumber("whatsapp:+9999999999"),
            new PhoneNumber("whatsapp:+" + _twilioPhoneNumber),
            "Foo message"
      ).create();

    return new ResponseEntity<>(message.toString(), HttpStatus.CREATED);
  }
````

The value “whatsapp/send” was defined as resourcePath in these steps.

Parameters:

*jwt*: this one will contains the JWT token that you can use to perform request to the liferay-portal to retrieve some informations about a resource for example:

````
HttpHeaders headers = new HttpHeaders();

headers.set(
HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());

ResponseEntity<String> response = new RestTemplate().exchange(
"https://dxp.lfr.dev/o/" + uri, HttpMethod.GET,
 new HttpEntity<String>(headers), String.class);
````

You can access the token through the method: getTokenValue()

*json*: this parameter will contain the variables that have already been mentioned here: notification template, user who triggered the notification, company and termValues.
