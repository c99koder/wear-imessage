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
var http = require('http');
var url = require('url') ;

http.createServer(function (req, res) {
    var queryObject = url.parse(req.url,true).query;
    if(queryObject != null && queryObject !== undefined && queryObject.handle !== undefined && queryObject.service !== undefined && queryObject.msg !== undefined) {
        console.log("Sending message to " + queryObject.handle + " on service ID " + queryObject.service);
        
        osascript.execute(
'tell application "Messages"\n\
	set targetService to 1st service whose id = theService\n\
	set targetBuddy to buddy theHandle of targetService\n\
	log targetBuddy\n\
	send theMessage to targetBuddy\n\
end tell', { theService:queryObject.service, theHandle:queryObject.handle, theMessage:queryObject.msg }, function(error, result, raw){
          if (error) {
              res.writeHead(500, {'Content-Type': 'text/plain'});
              res.end(error + "\n");
          } else {
              res.writeHead(200, {'Content-Type': 'text/plain'});
              res.end('Message sent to ' + queryObject.handle + "\n");
          }
        });
    } else {
        res.writeHead(404, {'Content-Type': 'text/plain'});
        res.end('Missing parameters\n');
    }
}).listen(1337);
console.log('Server running on port 1337');