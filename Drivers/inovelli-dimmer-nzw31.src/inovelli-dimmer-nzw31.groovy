/**
 *  Inovelli Dimmer NZW31
 *  Author: Eric Maycock (erocm123)
 *  Date: 2018-12-04
 *
 *  Copyright 2018 Eric Maycock
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  2020-11-15: Switch over to using Hubitat child device drivers - Kenneth Xu
 *
 *  2019-11-20: Fixed Association Group management.
 *              Added upgraded NZW31 fingerpint.
 *
 *  2019-09-02: Added Hubitat's method of disabling debug messages
 *
 *  2018-12-04: Added option to "Disable Remote Control" and to send button events 1,pushed / 1,held for on / off.
 *
 *  2018-06-20: Modified tile layout. Update firmware version reporting. Bug fix.
 * 
 *  2018-06-08: Remove communication method check from updated().
 *  
 *  2018-04-23: Added configuration parameters for association group 3.
 *
 *  2018-04-11: No longer deleting child devices when user toggles the option off. SmartThings was throwing errors.
 *              User will have to manually delete them.
 *
 *  2018-03-08: Added support for local protection to disable local control. Requires firmware 1.03+.
 *              Also merging handler from NZW31T as they are identical other than the LED indicator.
 *              Child device creation option added for local control setting. Child device must be installed:
 *              https://github.com/erocm123/SmartThingsPublic/blob/master/devicetypes/erocm123/switch-level-child-device.src
 *
 *  2018-03-01: Added support for additional configuration options (default level - local & z-wave) and child devices
 *              for adjusting configuration options from other SmartApps. Requires firmware 1.02+.
 *
 *  2018-02-26: Added support for Z-Wave Association Tool SmartApp. Associations require firmware 1.02+.
 *              https://github.com/erocm123/SmartThingsPublic/tree/master/smartapps/erocm123/z-waveat
 */
 
metadata {
    definition (
        name: "Inovelli Dimmer NZW31", 
        namespace: "InovelliUSA", 
        author: "Eric Maycock", 
        vid: "generic-dimmer",
        importUrl: "https://raw.githubusercontent.com/InovelliUSA/Hubitat/master/Drivers/inovelli-dimmer-nzw31.src/inovelli-dimmer-nzw31.groovy"
    ) {
        capability "Switch"
        capability "Refresh"
        capability "Polling"
        capability "Actuator"
        capability "Sensor"
        //capability "Health Check"
        capability "Switch Level"
        capability "Configuration"
        
        attribute "lastActivity", "String"
        attribute "lastEvent", "String"
        attribute "firmware", "String"
        
        command "setAssociationGroup", [[name: "Group Number*",type:"NUMBER", description: "Provide the association group number to edit"], 
                                        [name: "Z-Wave Node*", type:"STRING", description: "Enter the node number (in hex) associated with the node"], 
                                        [name: "Action*", type:"ENUM", constraints: ["Add", "Remove"]],
                                        [name:"Multi-channel Endpoint", type:"NUMBER", description: "Currently not implemented"]] 

        command "childOn"
        command "childOff"
        command "childRefresh"
        command "childSetLevel"
        command "componentOn"
        command "componentOff"
        command "componentSetLevel"
        command "componentRefresh"

        fingerprint mfr: "0312", prod: "0118", model: "1E1C", deviceJoinName: "Inovelli Dimmer"
        fingerprint mfr: "015D", prod: "0118", model: "1E1C", deviceJoinName: "Inovelli Dimmer"
        fingerprint mfr: "015D", prod: "1F01", model: "1F01", deviceJoinName: "Inovelli Dimmer"
        fingerprint mfr: "0312", prod: "1F01", model: "1F01", deviceJoinName: "Inovelli Dimmer"
        fingerprint deviceId: "0x1101", inClusters: "0x5E,0x26,0x27,0x70,0x75,0x22,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x6C,0x7A"
        fingerprint deviceId: "0x1F01", inClusters: "0x5E,0x26,0x70,0x75,0x22,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x9F,0x6C,0x7A" // Add for NZW31 upgrades to NZW31S
    }

    preferences {
        input "minimumLevel", "number", title: "Minimum Level\n\nMinimum dimming level for attached light\nRange: 1 to 40", description: "Tap to set", required: false, range: "1..40", defaultValue: "1"
        input "dimmingStep", "number", title: "Dimming Step Size\n\nPercentage of step when switch is dimming up or down\nRange: 0 to 99 (0 - Instant)", description: "Tap to set", required: false, range: "0..99", defaultValue: "3"
        input "autoOff", "number", title: "Auto Off\n\nAutomatically turn switch off after this number of seconds\nRange: 0 to 32767", description: "Tap to set", required: false, range: "0..32767", defaultValue: "0"
        input "ledIndicator", "enum", title: "LED Indicator\n\nTurn LED indicator on when light is: (Paddle Switch Only)", description: "Tap to set", required: false, options:[["1": "On"], ["0": "Off"], ["2": "Disable"], ["3": "Always On"]], defaultValue: "0"
        input "invert", "enum", title: "Invert Switch\n\nInvert on & off on the physical switch", description: "Tap to set", required: false, options:[["0": "No"], ["1": "Yes"]], defaultValue: "0"
        input "defaultLocal", "number", title: "Default Level (Local)\n\nDefault level when light is turned on at the switch\nRange: 0 to 99\nNote: 0 = Previous Level\n(Firmware 1.02+)", description: "Tap to set", required: false, range: "0..99", defaultValue: "0"
        input "defaultZWave", "number", title: "Default Level (Z-Wave)\n\nDefault level when light is turned on via Z-Wave command\nRange: 0 to 99\nNote: 0 = Previous Level\n(Firmware 1.02+)", description: "Tap to set", required: false, range: "0..99", defaultValue: "0"
        input "disableLocal", "enum", title: "Disable Local Control\n\nDisable ability to control switch from the wall\n(Firmware 1.03+)", description: "Tap to set", required: false, options:[["2": "Yes"], ["0": "No"]], defaultValue: "0"
        input "disableRemote", "enum", title: "Disable Remote Control\n\nDisable ability to control switch from inside Hubitat", description: "Tap to set", required: false, options:[["2": "Yes"], ["0": "No"]], defaultValue: "0"
        input "buttonOn", "enum", title: "Send Button Event On\n\nSend the button 1 pushed event when switch turned on from inside Hubitat", description: "Tap to set", required: false, options:[["1": "Yes"], ["0": "No"]], defaultValue: "0"
        input "buttonOff", "enum", title: "Send Button Event Off\n\nSend the button 1 held event when switch turned off from inside Hubitat", description: "Tap to set", required: false, options:[["1": "Yes"], ["0": "No"]], defaultValue: "0"
        input description: "Use the below options to enable child devices for the specified settings. This will allow you to adjust these settings using SmartApps such as Smart Lighting. If any of the options are enabled, make sure you have the appropriate child device handlers installed.\n(Firmware 1.02+)", title: "Child Devices", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input "enableDefaultLocalChild", "bool", title: "Default Local Level", description: "", required: false, defaultValue: false
        input "enableDefaultZWaveChild", "bool", title: "Default Z-Wave Level", description: "", required: false, defaultValue: false
        input "enableDisableLocalChild", "bool", title: "Disable Local Control", description: "", required: false, defaultValue: false
        input description: "Use the \"Z-Wave Association Tool\" SmartApp to set device associations.\n(Firmware 1.02+)\n\nGroup 2: Sends on/off commands to associated devices when switch is pressed (BASIC_SET).\n\nGroup 3: Sends dim/brighten commands to associated devices when switch is pressed (SWITCH_MULTILEVEL_SET).", title: "Associations", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input "group3Setting", "enum", title: "Association Group 3 Behavior\n\nChange how devices respond when associated in group 3", description: "Tap to set", required: false, options:[["1": "Keep in Sync"], ["0": "Dim up/down"]], defaultValue: "0"
        input description: "When should the switch send commands to associated devices?", title: "Association Behavior", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input "group3local", "bool", title: "Send command on local action", description: "", required: false, defaultValue: true
        input "group3remote", "bool", title: "Send command on z-wave action", description: "", required: false, defaultValue: true
        input "group3way", "bool", title: "Send command on 3-way action", description: "", required: false, defaultValue: true
        input "group3timer", "bool", title: "Send command on auto off timer", description: "", required: false, defaultValue: true
	    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def installed() {
    if (logEnable) log.debug "installed()"
    refresh()
}

def configure() {
    if (logEnable) log.debug "configure()"
    def cmds = initialize()
    commands(cmds)
}

def updated() {
    if (!state.lastRan || now() >= state.lastRan + 2000) {
        if (logEnable) log.debug "updated()"
        state.lastRan = now()
        def cmds = initialize()
        commands(cmds)
    } else {
        if (logEnable) log.debug "updated() ran within the last 2 seconds. Skipping execution."
    }
}

def integer2Cmd(value, size) {
    try{
	switch(size) {
	case 1:
		[value]
    break
	case 2:
    	def short value1   = value & 0xFF
        def short value2 = (value >> 8) & 0xFF
        [value2, value1]
    break
    case 3:
    	def short value1   = value & 0xFF
        def short value2 = (value >> 8) & 0xFF
        def short value3 = (value >> 16) & 0xFF
        [value3, value2, value1]
    break
	case 4:
    	def short value1 = value & 0xFF
        def short value2 = (value >> 8) & 0xFF
        def short value3 = (value >> 16) & 0xFF
        def short value4 = (value >> 24) & 0xFF
		[value4, value3, value2, value1]
	break
	}
    } catch (e) {
        if (logEnable) log.debug "Error: integer2Cmd $e Value: $value"
    }
}

private initChildDevice(id, enabled, label, driver, namespace = "hubitat", isComponent = false){
    def childDevice = childDevices.find{it.deviceNetworkId.endsWith(id)}
    if(enabled) {
        if(!childDevice) {
            try {
                def newChild = addChildDevice(namespace, driver, "${device.deviceNetworkId}-${id}",
                    [completedSetup: true, label: "${device.displayName} (${label})",
                    isComponent: isComponent, componentName: id, componentLabel: label])
                newChild.sendEvent(name:"switch", value:"off")
            } catch (e) {
                runIn(3, "sendAlert", [data: [message: "Child device creation failed. Make sure the driver for \"${driver}\" with a namespace of ${namespace} is installed"]])
            }
        } else if (device.label != state.oldLabel) {
             childDevice.setLabel("${device.displayName} (${label})")
        }
    } else if (childDevice) {
        try {
            deleteChildDevice(childDevice.deviceNetworkId)
        } catch (e) {
            if (infoEnable) log.info "Hubitat may have issues trying to delete the child device when it is in use. Need to manually delete them."
            runIn(3, "sendAlert", [data: [message: "Failed to delete child device. Make sure the device is not in use by any App."]])
        }
    }
}

def initialize() {
    sendEvent(name: "checkInterval", value: 3 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

    initChildDevice("ep8", enableDefaultLocalChild, "Default Local Level", "Generic Component Dimmer")
    initChildDevice("ep9", enableDefaultZWaveChild, "Default Z-Wave Level", "Generic Component Dimmer")
    initChildDevice("ep101", enableDisableLocalChild, "Disable Local Control", "Generic Component Switch")

    state.oldLabel = device.label

    def cmds = processAssociations()
    cmds << zwave.versionV1.versionGet()
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: dimmingStep!=null? dimmingStep.toInteger() : 1, parameterNumber: 1, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 1)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: minimumLevel!=null? minimumLevel.toInteger() : 1, parameterNumber: 2, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 2)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: ledIndicator!=null? ledIndicator.toInteger() : 0, parameterNumber: 3, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: invert!=null? invert.toInteger() : 0, parameterNumber: 4, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 4)
    cmds << zwave.configurationV1.configurationSet(configurationValue: autoOff!=null? integer2Cmd(autoOff.toInteger(), 2) : integer2Cmd(0,2), parameterNumber: 5, size: 2)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 5)
    if (state.defaultLocal != settings.defaultLocal) {
        cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: defaultLocal!=null? defaultLocal.toInteger() : 0, parameterNumber: 8, size: 1)
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 8)
    }
    if (state.defaultZWave != settings.defaultZWave) {
        cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: defaultZWave!=null? defaultZWave.toInteger() : 0, parameterNumber: 9, size: 1)
        cmds << zwave.configurationV1.configurationGet(parameterNumber: 9)
    }
    if (state.disableLocal != settings.disableLocal) {
        cmds << zwave.protectionV2.protectionSet(localProtectionState : disableLocal!=null? disableLocal.toInteger() : 0, rfProtectionState: 0)
        cmds << zwave.protectionV2.protectionGet()
    }
    
    //Calculate group 3 configuration parameter
    def group3value = 0
    group3value += group3local? 1 : 0
    group3value += group3way? 2 : 0
    group3value += group3remote? 4 : 0 
    group3value += group3timer? 8 : 0
    
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: group3value, parameterNumber: 6, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 6)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: group3Setting!=null? group3Setting.toInteger() : 0, parameterNumber: 7, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 7)
    
    state.defaultLocal = settings.defaultLocal
    state.defaultZWave = settings.defaultZWave
    state.disableLocal = settings.disableLocal
    return cmds
}

def parse(description) {
    def result = null
    if (description.startsWith("Err 106")) {
        state.sec = 0
        result = createEvent(descriptionText: description, isStateChange: true)
    } else if (description != "updated") {
        def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x70: 1, 0x98: 1])
        if (cmd) {
            result = zwaveEvent(cmd)
            if (logEnable) log.debug("'$description' parsed to $result")
        } else {
            if (logEnable) log.debug("Couldn't zwave.parse '$description'")
        }
    }
    def now
    if(location.timeZone)
    now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    else
    now = new Date().format("yyyy MMM dd EEE h:mm:ss a")
    sendEvent(name: "lastActivity", value: now, displayed:false)
    result
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    // Since Hubitat isn't filtering duplicate events, we are skipping these
    // Switch is sending SwitchMultilevelReport as well (which we will use)
    //dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
    dimmerEvents(cmd)
}

private dimmerEvents(hubitat.zwave.Command cmd) {
    def value = (cmd.value ? "on" : "off")
    def result = [createEvent(name: "switch", value: value)]
    if (cmd.value) {
        result << createEvent(name: "level", value: cmd.value, unit: "%")
    }
    return result
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    if (logEnable) log.debug "${device.displayName} parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd2Integer(cmd.configurationValue)}'"
    def integerValue = cmd2Integer(cmd.configurationValue)
    switch (cmd.parameterNumber) {
        case 8:
            def children = childDevices
            def childDevice = children.find{it.deviceNetworkId.endsWith("ep8")}
            if (childDevice) {
            childDevice.sendEvent(name: "switch", value: integerValue > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: integerValue)            
            }
        break
        case 9:
            def children = childDevices
            def childDevice = children.find{it.deviceNetworkId.endsWith("ep9")}
            if (childDevice) {
            childDevice.sendEvent(name: "switch", value: integerValue > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: integerValue)
            }
        break
    }
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x25: 1])
    if (encapsulatedCommand) {
        state.sec = 1
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    if (logEnable) log.debug "Unhandled: $cmd"
    null
}

def on() {
    if (buttonOn == "1") {
        sendEvent([name: "pushed", value: 1, isStateChange:true])
    }
    if (disableRemote != "2") {
    commands([
        zwave.basicV1.basicSet(value: 0xFF),
        zwave.switchMultilevelV1.switchMultilevelGet()
    ])
    } else {
    commands([
        zwave.switchMultilevelV1.switchMultilevelGet()
    ])
    }
}

def off() {
    if (buttonOff == "1") {
        sendEvent([name: "held", value: 1, isStateChange:true])
    }
    if (disableRemote != "2") {
    commands([
        zwave.basicV1.basicSet(value: 0x00),
        zwave.switchMultilevelV1.switchMultilevelGet()
    ])
    } else {
    commands([
        zwave.switchMultilevelV1.switchMultilevelGet()
    ])
    }
}

def setLevel(value) {
    if (disableRemote != "2") {
    commands([
        zwave.basicV1.basicSet(value: value < 100 ? value : 99),
        zwave.switchMultilevelV1.switchMultilevelGet()
    ])
    } else {
    commands([
        zwave.switchMultilevelV1.switchMultilevelGet()
    ])
    }
}

def setLevel(value, duration) {
    if (disableRemote != "2") {
    def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
        commands([
            zwave.switchMultilevelV2.switchMultilevelSet(value: value < 100 ? value : 99, dimmingDuration: dimmingDuration),
            zwave.switchMultilevelV1.switchMultilevelGet()
        ])
    } else {
    commands([
        zwave.switchMultilevelV1.switchMultilevelGet()
    ])
    }
}

def componentSetLevel(cd,level,transitionTime = null) {
    if (infoEnable) log.info "${device.label?device.label:device.name}: componentSetLevel($cd, $value)"
    int channel = cd.deviceNetworkId.split("-ep")[-1] as Integer
    int value = Math.max(Math.min(level as Integer, 99), 0)
    def cmds = []
    switch (channel) {
        case 8:
            cmds << new hubitat.device.HubAction(command(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: channel, size: 1) ), hubitat.device.Protocol.ZWAVE)
            cmds << new hubitat.device.HubAction(command(zwave.configurationV1.configurationGet(parameterNumber: channel)), hubitat.device.Protocol.ZWAVE)
        break
        case 9:
            cmds << new hubitat.device.HubAction(command(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: channel, size: 1) ), hubitat.device.Protocol.ZWAVE)
            cmds << new hubitat.device.HubAction(command(zwave.configurationV1.configurationGet(parameterNumber: channel)), hubitat.device.Protocol.ZWAVE)
        break
        case 101:
            cmds << new hubitat.device.HubAction(command(zwave.protectionV2.protectionSet(localProtectionState : value > 0 ? 2 : 0, rfProtectionState: 0) ), hubitat.device.Protocol.ZWAVE)
            cmds << new hubitat.device.HubAction(command(zwave.protectionV2.protectionGet() ), hubitat.device.Protocol.ZWAVE)
        break
    }
	cmds
}

def componentOn(cd) {
    if (infoEnable) log.info "${device.label?device.label:device.name}: componentOn($cd)"
    return componentSetLevel(cd, 99)
}

def componentOff(cd) {
    if (infoEnable) log.info "${device.label?device.label:device.name}: componentOff($cd)"
    return componentSetLevel(cd, 0)
}

void componentRefresh(cd) {
    if (infoEnable) log.info "${device.label?device.label:device.name}: componentRefresh($cd)"
}

def cmd2Integer(array) {
    switch(array.size()) {
        case 1:
            array[0]
            break
        case 2:
            ((array[0] & 0xFF) << 8) | (array[1] & 0xFF)
            break
        case 3:
            ((array[0] & 0xFF) << 16) | ((array[1] & 0xFF) << 8) | (array[2] & 0xFF)
            break
        case 4:
            ((array[0] & 0xFF) << 24) | ((array[1] & 0xFF) << 16) | ((array[2] & 0xFF) << 8) | (array[3] & 0xFF)
            break
    }
}

def childExists(ep) {
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith(ep)}
    if (childDevice) 
        return true
    else
        return false
}

def ping() {
    if (logEnable) log.debug "ping()"
    refresh()
}

def poll() {
    if (logEnable) log.debug "poll()"
    refresh()
}

def refresh() {
    if (logEnable) log.debug "refresh()"
    commands([zwave.switchBinaryV1.switchBinaryGet(),
              zwave.switchMultilevelV1.switchMultilevelGet()
    ])
}

private command(hubitat.zwave.Command cmd) {
    if (state.sec) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private commands(commands, delay=500) {
    delayBetween(commands.collect{ command(it) }, delay)
}

def setDefaultAssociations() {
    def smartThingsHubID = String.format('%02x', zwaveHubNodeId).toUpperCase()
    state.defaultG1 = [smartThingsHubID]
    state.defaultG2 = []
    state.defaultG3 = []
}

def setAssociationGroup(group, nodes, action, endpoint = null){
    // Normalize the arguments to be backwards compatible with the old method
    action = "${action}" == "1" ? "Add" : "${action}" == "0" ? "Remove" : "${action}" // convert 1/0 to Add/Remove
    group  = "${group}" =~ /\d+/ ? (group as int) : group                             // convert group to int (if possible)
    nodes  = [] + nodes ?: [nodes]                                                    // convert to collection if not already a collection

    if (! nodes.every { it =~ /[0-9A-F]+/ }) {
        log.error "invalid Nodes ${nodes}"
        return
    }

    if (group < 1 || group > maxAssociationGroup()) {
        log.error "Association group is invalid 1 <= ${group} <= ${maxAssociationGroup()}"
        return
    }
    
    def associations = state."desiredAssociation${group}"?:[]
    nodes.each { 
        node = "${it}"
        switch (action) {
            case "Remove":
            if (logEnable) log.debug "Removing node ${node} from association group ${group}"
            associations = associations - node
            break
            case "Add":
            if (logEnable) log.debug "Adding node ${node} to association group ${group}"
            associations << node
            break
        }
    }
    state."desiredAssociation${group}" = associations.unique()
    return
}

def maxAssociationGroup(){
   if (!state.associationGroups) {
       if (logEnable) log.debug "Getting supported association groups from device"
       zwave.associationV2.associationGroupingsGet() // execute the update immediately
   }
   (state.associationGroups?: 5) as int
}

def processAssociations(){
   def cmds = []
   setDefaultAssociations()
   def associationGroups = maxAssociationGroup()
   for (int i = 1; i <= associationGroups; i++){
      if(state."actualAssociation${i}" != null){
         if(state."desiredAssociation${i}" != null || state."defaultG${i}") {
            def refreshGroup = false
            ((state."desiredAssociation${i}"? state."desiredAssociation${i}" : [] + state."defaultG${i}") - state."actualAssociation${i}").each {
                if (logEnable) log.debug "Adding node $it to group $i"
                cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:hubitat.helper.HexUtils.hexStringToInt(it))
                refreshGroup = true
            }
            ((state."actualAssociation${i}" - state."defaultG${i}") - state."desiredAssociation${i}").each {
                if (logEnable) log.debug "Removing node $it from group $i"
                cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId:hubitat.helper.HexUtils.hexStringToInt(it))
                refreshGroup = true
            }
            if (refreshGroup == true) cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
            else if (logEnable) log.debug "There are no association actions to complete for group $i"
         }
      } else {
         if (logEnable) log.debug "Association info not known for group $i. Requesting info from device."
         cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
      }
   }
   return cmds
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    def temp = []
    if (cmd.nodeId != []) {
       cmd.nodeId.each {
          temp += it.toString().format( '%02x', it.toInteger() ).toUpperCase()
       }
    } 
    state."actualAssociation${cmd.groupingIdentifier}" = temp
    if (logEnable) log.debug "Associations for Group ${cmd.groupingIdentifier}: ${temp}"
    updateDataValue("associationGroup${cmd.groupingIdentifier}", "$temp")
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    sendEvent(name: "groups", value: cmd.supportedGroupings)
    if (logEnable) log.debug "Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
    if (logEnable) log.debug cmd
    if(cmd.applicationVersion && cmd.applicationSubVersion) {
	    def firmware = "${cmd.applicationVersion}.${cmd.applicationSubVersion.toString().padLeft(2,'0')}"
        state.needfwUpdate = "false"
        createEvent(name: "firmware", value: "${firmware}")
    }
}

def zwaveEvent(hubitat.zwave.commands.protectionv2.ProtectionReport cmd) {
    if (logEnable) log.debug cmd
    def integerValue = cmd.localProtectionState
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith("ep101")}
    if (childDevice) {
        childDevice.sendEvent(name: "switch", value: integerValue > 0 ? "on" : "off")        
    }
}
