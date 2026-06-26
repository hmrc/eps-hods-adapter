# eps-hods-adapter
# Overview

Facilitates other eps mircoservices with NPS communications and receives alerts from NPS.

# API
==================

Method | Endpoint | Description
-------|----------|---------------------------------------
 GET | `/preferences/person/:nino` | Gets a user from NPS [More](#get-preferencespersonnino)
 POST | `/preferences/person/:nino/print-suppression` | Updates the Print Suppression status of the user on NPS [More](#post-preferencesninoprint-suppression)
 PUT | `/preferences/alert` | NPS sends alerts synchronously to this endpoint and they are persisted in mongo [More](#put-preferencesalert)
 POST | `/preferences/alert/print-suppression/:id/status` | Updates the processing status of an alert after sending an alert/email to the customer [More](#post-preferencesalertprint-suppressionidstatus)
 GET | `/preferences/alert/print-suppression/:id/status` | Gets the processing status of the alert with the passed id [More](#get-preferencesalertprint-suppressionidstatus)


### GET /preferences/person/:nino

See [eps-hods-stubs](https://github.tools.tax.service.gov.uk/DDCN/eps-hods-stubs#get-pay-as-you-earnindividualsnino) for more information on response.


### POST /preferences/person/:nino/print-suppression

Updates the Print Suppression status of the user on NPS.

Example request:

```javascript
{
    "formType": "P2",
    "outputPreference": "digital",
    "bounced": false
}
```

The body is an `NpsPrintSuppressionUpdateRequest` object

Responds with status:
* `200` if the print suppression has been updated successfully on NPS

Example response:

```javascript
{
    "rejectionCode": 0
}
```


### PUT /preferences/alert

NPS sends alerts synchronously to this endpoint and they are persisted in mongo.

Example request:

```javascript
{
    "alert" : {
        "identifier": {
            "idType": "nino",
            "value": "QQ123456C"
        },
        "hodId": "nps",
        "templateId": "0004"
    }
}
```

The body is a `NpsAlert` object wrapped in an `Alert` object

Responds with status:
* `202` if the alert is accepted and has been persisted. The actual sending of the alerts is asynchronous which is why we send a `202` not a `200` as we can only guarantee to the sender that we have perisisted the alert, not processed it.

### POST /preferences/alert/print-suppression/:id/status

Updates the processing status of an alert after sending an alert/email to the customer.

Example request:

```javascript
{
    "status": "Succeeded"
}
```

The value of `status` can be:
* `Succeeded` if the alert has been successfully sent
* `Failed` if processing the alert has not been successful and we think it can be fixed/will work if we send again
* `PermanentlyFailed` if processing has failed and there is no way to recover and send this alert out

Responds with status:
* `200` if the work item was updated with the passed status


### GET /preferences/alert/print-suppression/:id/status

Gets the processing status of the alert with the passed id.

Example request:

```javascript
{
    "status": "Succeeded"
}
```

Responds with status:
* `200` if the status update has been successful
