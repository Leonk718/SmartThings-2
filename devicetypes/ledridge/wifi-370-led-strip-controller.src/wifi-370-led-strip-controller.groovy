/**
 *  RGBWW LED Controller
 *
 *  Copyright 2017 Ph4r
 *
 */
metadata {
	definition (name: "WiFi 370 LED Strip Controller", namespace: "Ph4r", author: "Ph4r") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
		capability "Switch"
		capability "Refresh"
		capability "Sensor"
        
		command "setAdjustedColor"
		command "refresh"
	}
    
preferences {
	input("ip", "string", title:"Controller IP Address", description: "Controller IP Address", defaultValue: "192.168.1.69", required: true, displayDuringSetup: true)
	input("port", "string", title:"Controller Port", description: "Controller Port", defaultValue: 5577 , required: true, displayDuringSetup: true)
	input("username", "string", title:"Controller Username", description: "Controller Username", defaultValue: admin, required: true, displayDuringSetup: true)
	input("password", "password", title:"Controller Password", description: "Controller Password", defaultValue: nimda, required: true, displayDuringSetup: true)
	}

	standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
		state "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821"
		state "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff"
	}
	standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
		state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
	}
	controlTile("rgbSelector", "device.color", "color", height: 3, width: 3, inactiveLabel: false) {
		state "color", action:"setAdjustedColor"
	}
	controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range:"(0..100)") {
		state "level", action:"switch level.setLevel"
	}
	valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
		state "level", label: 'Level ${currentValue}%'
	}
	controlTile("saturationSliderControl", "device.saturation", "slider", height: 1, width: 2, inactiveLabel: false) {
		state "saturation", action:"color control.setSaturation"
	}
	valueTile("saturation", "device.saturation", inactiveLabel: false, decoration: "flat") {
		state "saturation", label: 'Sat ${currentValue}    '
	}
	controlTile("hueSliderControl", "device.hue", "slider", height: 1, width: 2, inactiveLabel: false) {
		state "hue", action:"color control.setHue"
	}
	valueTile("hue", "device.hue", inactiveLabel: false, decoration: "flat") {
		state "hue", label: 'Hue ${currentValue}   '
	}

	main(["switch"])
	details(["switch", "rgbSelector", "refresh"])

}

// parse events into attributes
def parse(description) {
	log.debug "parse() - $description"
	def results = []
	def map = description
	if (description instanceof String)  {
		log.debug "Hue Bulb stringToMap - ${map}"
		map = stringToMap(description)
	}
	if (map?.name && map?.value) {
		results << createEvent(name: "${map?.name}", value: "${map?.value}")
	}
	results
}

// handle commands
def on() {
	sendEvent(name: "switch", value: "on")
    sendPower(true)
}

def off() {
	sendEvent(name: "switch", value: "off")
    sendPower(false)
}

def sendPower(state) {
	byte[] bytes = [0x71, 0x24, 0x0F, 0xA4]
    if (state) { // 71 23 0f a3 on
    	bytes = [0x71, 0x23, 0x0F, 0xA3]
    } else {	 //71 24 0f a4 off
    	bytes = [0x71, 0x24, 0x0F, 0xA4]
	}
    String body = bytes.encodeHex()
    String sData = new String(bytes, "ISO-8859-1");
    byte[] bytes2 = [0xA4]
    String sData2 = new String(bytes2, "ISO-8859-1");
    String pure = "\u7123\u0FA4"
    log.debug "${sData}:${sData2}:${pure}:${body}"
    //sendHubCommand(new physicalgraph.device.HubAction(sData, physicalgraph.device.Protocol.LAN, getDataValue("mac"))); //"0A0A0A15:15C9"
    //sendHubCommand(new physicalgraph.device.HubAction(sData2, physicalgraph.device.Protocol.LAN, getDataValue("mac"))); //"0A0A0A15:15C9"
    //sendHubCommand(new physicalgraph.device.HubAction(pure, physicalgraph.device.Protocol.LAN, getDataValue("mac"))); //"0A0A0A15:15C9"
    sendHubCommand(new physicalgraph.device.HubAction(body.toString(), physicalgraph.device.Protocol.LAN, getDataValue("mac"))); //"0A0A0A15:15C9"
    
}

def sendChange(state) {
	byte[] byteHeader = [0x31]
    byte[] byteFooter = [0x00, 0x00, 0xF0, 0x0F]
    
    String bodyHeader = byteHeader.encodeHex()
    String bodyFooter = byteFooter.encodeHex()
    String bodyMain = bodyHeader + state.reverse().take(6).reverse() + bodyFooter
    
    def byteMain = bodyMain.decodeHex()
    def checksum = 0
    
    byteMain.each {
    	checksum += it;
    }
    checksum = checksum & 0xFF
    String checksumHex = Integer.toHexString(checksum)
    //log.debug "${checksum}:${checksumHex}"
    
    String body = bodyMain + checksumHex
    
    sendHubCommand(new physicalgraph.device.HubAction(body.toString(), physicalgraph.device.Protocol.LAN, getDataValue("mac"))); //"0A0A0A15:15C9"
    
}

def nextLevel() {
	def level = device.latestValue("level") as Integer ?: 0
	if (level <= 100) {
		level = Math.min(25 * (Math.round(level / 25) + 1), 100) as Integer
	}
	else {
		level = 25
	}
	setLevel(level)
}

def setLevel(percent) {
	log.debug "Executing 'setLevel'"
	//parent.setLevel(this, percent)
	sendEvent(name: "level", value: percent)
}

def setSaturation(percent) {
	log.debug "Executing 'setSaturation'"
	parent.setSaturation(this, percent)
	sendEvent(name: "saturation", value: percent)
}

def setHue(percent) {
	log.debug "Executing 'setHue'"
	parent.setHue(this, percent)
	sendEvent(name: "hue", value: percent)
}

def setColor(value) {
	log.debug "setColor: ${value}, $this"
    sendChange(value.hex)
	//parent.setColor(this, value)
	if (value.hue) { sendEvent(name: "hue", value: value.hue)}
	if (value.saturation) { sendEvent(name: "saturation", value: value.saturation)}
	if (value.hex) { sendEvent(name: "color", value: value.hex)}
	if (value.level) { sendEvent(name: "level", value: value.level)}
	if (value.switch) { sendEvent(name: "switch", value: value.switch)}
}

def setAdjustedColor(value) {
	if (value) {
        log.debug "setAdjustedColor: ${value}"
        def adjusted = value + [:]
        adjusted.hue = adjustOutgoingHue(value.hue)
        // Needed because color picker always sends 100
        adjusted.level = null 
        setColor(adjusted)
    }
}

def refresh() {
	log.debug "Executing 'refresh'"
	parent.poll()
}

def adjustOutgoingHue(percent) {
	def adjusted = percent
	if (percent > 31) {
		if (percent < 63.0) {
			adjusted = percent + (7 * (percent -30 ) / 32)
		}
		else if (percent < 73.0) {
			adjusted = 69 + (5 * (percent - 62) / 10)
		}
		else {
			adjusted = percent + (2 * (100 - percent) / 28)
		}
	}
	log.info "percent: $percent, adjusted: $adjusted"
	adjusted
}

private hubGet(def apiCommand) {
	//Setting Network Device Id
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
    device.deviceNetworkId = "$iphex:$porthex"
    log.debug "Device Network Id set to ${iphex}:${porthex}"

	log.debug("Executing hubaction on " + getHostAddress())
    def uri = ""
    if(hdcamera == "true") {
    	uri = "/cgi-bin/CGIProxy.fcgi?" + getLogin() + apiCommand
	}
    else {
    	uri = apiCommand + getLogin()
    }
    log.debug uri
    def hubAction = new physicalgraph.device.HubAction(
    	method: "GET",
        path: uri,
        headers: [HOST:getHostAddress()]
    )
    if(device.currentValue("hubactionMode") == "s3") {
        hubAction.options = [outputMsgToS3:true]
        sendEvent(name: "hubactionMode", value: "local");
    }
	hubAction
}

//Parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
    
    def map = [:]
    def retResult = []
    def descMap = parseDescriptionAsMap(description)
        
    //Image
	if (descMap["bucket"] && descMap["key"]) {
		putImageInS3(descMap)
	}

	//Status Polling
    else if (descMap["headers"] && descMap["body"]) {
        def body = new String(descMap["body"].decodeBase64())
        if(hdcamera == "true") {
            def langs = new XmlSlurper().parseText(body)

            def motionAlarm = "$langs.isEnable"
            def ledMode = "$langs.mode"

            //Get Motion Alarm Status
            if(motionAlarm == "0") {
                log.info("Polled: Alarm Off")
                sendEvent(name: "alarmStatus", value: "off");
            }
            else if(motionAlarm == "1") {
                log.info("Polled: Alarm On")
                sendEvent(name: "alarmStatus", value: "on");
            }

            //Get IR LED Mode
            if(ledMode == "0") {
                log.info("Polled: LED Mode Auto")
                sendEvent(name: "ledStatus", value: "auto")
            }
            else if(ledMode == "1") {
                log.info("Polled: LED Mode Manual")
                sendEvent(name: "ledStatus", value: "manual")
            }
    	}
        else {
        	if(body.find("alarm_motion_armed=0")) {
				log.info("Polled: Alarm Off")
                sendEvent(name: "alarmStatus", value: "off")
            }
        	else if(body.find("alarm_motion_armed=1")) {
				log.info("Polled: Alarm On")
                sendEvent(name: "alarmStatus", value: "on")
            }
            //The API does not provide a way to poll for LED status on 8xxx series at the moment
        }
	}
}

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}

private getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}
