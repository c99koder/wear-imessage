/*
 * Copyright (c) 2015 Sam Steele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var osascript = require('node-osascript');
var express = require('express');
var app = express();

app.get('/send', function (req, res) {
    if(req.query.handle !== undefined && req.query.service !== undefined && req.query.msg !== undefined) {
        console.log("Sending message to " + req.query.handle + " on service ID " + req.query.service);
        
        osascript.execute(
'tell application "Messages"\n\
	set targetService to 1st service whose id = theService\n\
	set targetBuddy to buddy theHandle of targetService\n\
	send theMessage to targetBuddy\n\
end tell', { theService:req.query.service, theHandle:req.query.handle, theMessage:req.query.msg }, function(error, result, raw){
          if (error) {
              res.writeHead(500, {'Content-Type': 'text/plain'});
              res.end(error + "\n");
          } else {
              res.writeHead(200, {'Content-Type': 'text/plain'});
              res.end('OK\n");
          }
        });
    } else {
        res.writeHead(404, {'Content-Type': 'text/plain'});
        res.end('Missing parameters\n');
    }
});

app.get('/buddies', function (req, res) {
        osascript.execute(
'tell application "Messages"\n\
    set theResult to "["\n\
    repeat with theBuddy in buddies\n\
        set theResult to theResult & "{"\n\
        set theResult to theResult & "\\"name\\":\\"" & name of theBuddy & "\\","\n\
        set theResult to theResult & "\\"service\\":\\"" & id of service of theBuddy & "\\","\n\
        set theResult to theResult & "\\"service_type\\":\\"" & service type of service of theBuddy & "\\","\n\
        set theResult to theResult & "\\"handle\\":\\"" & handle of theBuddy & "\\","\n\
        set theResult to theResult & "\\"status\\":\\"" & status of theBuddy & "\\""\n\
        set theResult to theResult & "}, "\n\
    end repeat\n\
    return (text 1 thru ((count of characters of theResult) - 2) of theResult) & "]"\n\
end tell', { }, function(error, result, raw) {
    if (error) {
        res.writeHead(500, {'Content-Type': 'text/plain'});
        res.end(error + "\n");
    } else {
        res.writeHead(200, {'Content-Type': 'text/plain'});
        res.end(result);
    }
        });
});

app.listen(1337);
console.log('Server running on port 1337');
