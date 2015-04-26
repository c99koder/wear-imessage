(*
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
 *)

on push(theService, theHandle, theFullName, theMessage, theType)
	set API_KEY to "YOUR-API-KEY-HERE"
	set REG_ID to "YOUR-GCM-ID-HERE"
	
	do shell script "curl --header \"Authorization: key=" & API_KEY & "\" -F registration_id=" & REG_ID & " -F data.service=" & quoted form of theService & " -F data.handle=" & quoted form of theHandle & " -F data.name=" & quoted form of theFullName & " -F data.msg=" & quoted form of theMessage & " -F data.type=" & quoted form of theType & " https://android.googleapis.com/gcm/send"
	return
end push

using terms from application "Messages"
	on message received theMessage from theBuddy for theChat
		if contents of theMessage is not "" then
			my push((id of service of theBuddy), (handle of theBuddy), (full name of theBuddy), theMessage, "msg")
		end if
	end message received
	
	on chat room message received theMessage from theBuddy for theChat
		if contents of theMessage is not "" then
			my push((id of service of theBuddy), (handle of theBuddy), (full name of theBuddy), theMessage, "msg")
		end if
	end chat room message received
	
	on addressed chat room message received theMessage from theBuddy for theChat
		if contents of theMessage is not "" then
			my push((id of service of theBuddy), (handle of theBuddy), (full name of theBuddy), theMessage, "msg")
		end if
	end addressed chat room message received
	
	on addressed message received theMessage from theBuddy for theChat
		if contents of theMessage is not "" then
			my push((id of service of theBuddy), (handle of theBuddy), (full name of theBuddy), theMessage, "msg")
		end if
	end addressed message received
	
	on active chat message received theMessage from theBuddy for theChat
		if contents of theMessage is not "" then
			my push((id of service of theBuddy), (handle of theBuddy), (full name of theBuddy), theMessage, "msg")
		end if
	end active chat message received
	
	on received text invitation theText from theBuddy for theChat
		if contents of theMessage is not "" then
			my push((id of service of theBuddy), (handle of theBuddy), (full name of theBuddy), theMessage, "msg")
		end if
	end received text invitation
	
	on completed file transfer theFile
		if direction of theFile is incoming then
			my push((id of service of buddy of theFile), (handle of buddy of theFile), (full name of buddy of theFile), (id of theFile), "file")
		end if
	end completed file transfer
	
	# The following are unused but need to be defined to avoid an error
	
	on message sent theMessage for theChat
		
	end message sent
	
	on received audio invitation theText from theBuddy for theChat
		
	end received audio invitation
	
	on received video invitation theText from theBuddy for theChat
		
	end received video invitation
	
	on received remote screen sharing invitation from theBuddy for theChat
		
	end received remote screen sharing invitation
	
	on received local screen sharing invitation from theBuddy for theChat
		
	end received local screen sharing invitation
	
	on received file transfer invitation theFileTransfer
		
	end received file transfer invitation
	
	on buddy authorization requested theRequest
		
	end buddy authorization requested
	
	on av chat started
		
	end av chat started
	
	on av chat ended
		
	end av chat ended
	
	on login finished for theService
		
	end login finished
	
	on logout finished for theService
		
	end logout finished
	
	on buddy became available theBuddy
		
	end buddy became available
	
	on buddy became unavailable theBuddy
		
	end buddy became unavailable
	
end using terms from