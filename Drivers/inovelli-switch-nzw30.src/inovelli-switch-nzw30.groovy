/**
 *  Inovelli Switch NZW30
 *  Author: Eric Maycock (erocm123)
 *  Date: 2018-12-04
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
 *
 *  2019-07-09: Removed unused simulator and tiles sections
 *
 *  2018-12-04: Added option to "Disable Remote Control" and to send button events 1,pushed / 1,held for on / off.
 *
 *  2018-06-20: Modified tile layout. Update firmware version reporting. Bug Fix.
 * 
 *  2018-06-08: Remove communication method check from updated().
 * 
 *  2018-04-11: No longer deleting child devices when user toggles the option off. SmartThings was throwing errors.
 *              User will have to manually delete them.
 *
 *  2018-03-08: Added support for local protection to disable local control. Requires firmware 1.03+.
 *              Also merging handler from NZW30T as they are identical other than the LED indicator.
 *              Child device creation option added for local control setting. Child device must be installed:
 *              https://github.com/erocm123/SmartThingsPublic/blob/master/devicetypes/erocm123/switch-level-child-device.src
 *       
 *  2018-02-26: Added support for Z-Wave Association Tool SmartApp. Associations require firmware 1.02+.
 *              https://github.com/erocm123/SmartThingsPublic/tree/master/smartapps/erocm123/z-waveat
 */
 
metadata {
	definition (
        name: "Inovelli Switch NZW30", 
        namespace: "InovelliUSA", 
        author: "Eric Maycock", 
        vid: "generic-switch",
        importUrl: "https://raw.githubusercontent.com/InovelliUSA/Hubitat/master/Drivers/inovelli-switch-nzw30.src/inovelli-switch-nzw30.groovy"
    ) {
        capability "Switch"
        capability "Refresh"
        capability "Polling"
        capability "Actuator"
        capability "Sensor"
        //capability "Health Check"
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
 
        fingerprint mfr: "0312", prod: "0117", model: "1E1C", deviceJoinName: "Inovelli Switch"
        fingerprint mfr: "015D", prod: "0117", model: "1E1C", deviceJoinName: "Inovelli Switch"
        fingerprint mfr: "015D", prod: "1E01", model: "1E01", deviceJoinName: "Inovelli Switch"
        fingerprint mfr: "0312", prod: "1E01", model: "1E01", deviceJoinName: "Inovelli Switch"
        fingerprint deviceId: "0x1001", inClusters: "0x5E,0x86,0x72,0x5A,0x85,0x59,0x73,0x25,0x27,0x70,0x75,0x22,0x8E,0x55,0x6C,0x7A"
    }

    preferences {
        input "autoOff", "number", title: "Auto Off\n\nAutomatically turn switch off after this number of seconds\nRange: 0 to 32767", description: "Tap to set", required: false, range: "0..32767", defaultValue: "0"
        input "ledIndicator", "enum", title: "LED Indicator\n\nTurn LED indicator on when light is: (Paddle Switch Only)\n", description: "Tap to set", required: false, options:[["1": "On"], ["0": "Off"], ["2": "Disable"], ["3": "Always On"]], defaultValue: "0"
        input "invert", "enum", title: "Invert Switch\n\nInvert on & off on the physical switch", description: "Tap to set", required: false, options:[["0": "No"], ["1": "Yes"]], defaultValue: "0"
        input "disableLocal", "enum", title: "Disable Local Control\n\nDisable ability to control switch from the wall\n(Firmware 1.02+)", description: "Tap to set", required: false, options:[["2": "Yes"], ["0": "No"]], defaultValue: "0"
        input "disableRemote", "enum", title: "Disable Remote Control\n\nDisable ability to control switch from inside Hubitat", description: "Tap to set", required: false, options:[["2": "Yes"], ["0": "No"]], defaultValue: "0"
        input "buttonOn", "enum", title: "Send Button Event On\n\nSend the button 1 pushed event when switch turned on from inside Hubitat", description: "Tap to set", required: false, options:[["1": "Yes"], ["0": "No"]], defaultValue: "0"
        input "buttonOff", "enum", title: "Send Button Event Off\n\nSend the button 1 held event when switch turned off from inside Hubitat", description: "Tap to set", required: false, options:[["1": "Yes"], ["0": "No"]], defaultValue: "0"
        input description: "Use the below options to enable child devices for the specified settings. This will allow you to adjust these settings using SmartApps such as Smart Lighting. If any of the options are enabled, make sure you have the appropriate child device handlers installed.\n(Firmware 1.02+)", title: "Child Devices", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input "enableDisableLocalChild", "bool", title: "Disable Local Control", description: "", required: false
        input description: "Use the \"Z-Wave Association Tool\" SmartApp to set device associations. (Firmware 1.02+)\n\nGroup 2: Sends on/off commands to associated devices when switch is pressed (BASIC_SET).", title: "Associations", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true, required: true
    }
}

def installed() {
    if (logEnabled) log.debug "installed()"
    refresh()
}

def configure() {
    if (logEnabled) log.debug "configure()"
    def cmds = initialize()
    commands(cmds)
}

def updated() {
    if (!state.lastRan || now() >= state.lastRan + 2000) {
        if (logEnabled) log.debug "updated()"
        state.lastRan = now()
        def cmds = initialize()
        commands(cmds)
    } else {
        if (logEnabled) log.debug "updated() ran within the last 2 seconds. Skipping execution."
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
        if (logEnabled) log.debug "Error: integer2Cmd $e Value: $value"
    }
}

def initialize() {
    sendEvent(name: "checkInterval", value: 3 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    if (enableDisableLocalChild && !childExists("ep101")) {
        try {
            addChildDevice("hubitat", "Generic Component Switch", "${device.deviceNetworkId}-ep101",
                    [completedSetup: true, label: "${device.displayName} (Disable Local Control)",
                    isComponent: false, componentName: "ep101", componentLabel: "Disable Local Control"])
        } catch (e) {
            runIn(3, "sendAlert", [data: [message: "Child device creation failed. Make sure the device handler for \"Switch Level Child Device\" is installed"]])
        }
    } else if (!enableDisableLocalChild && childExists("ep101")) {
        if (logEnabled) log.debug "Trying to delete child device ep101. If this fails it is likely that there is a SmartApp using the child device in question."
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("ep101")}
        try {
            if (logEnabled) log.debug "Hubitat has issues trying to delete the child device when it is in use. Need to manually delete them."
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
        } catch (e) {
            runIn(3, "sendAlert", [data: [message: "Failed to delete child device. Make sure the device is not in use by any SmartApp."]])
        }
    }
    if (device.label != state.oldLabel) {
        def children = childDevices
        def childDevice = children.find{it.deviceNetworkId.endsWith("e101")}
        if (childDevice)
            childDevice.setLabel("${device.displayName} (Disable Local Control)")
        state.oldLabel = device.label
    }
    
    def cmds = processAssociations()
    cmds << zwave.versionV1.versionGet()
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: ledIndicator!=null? ledIndicator.toInteger() : 0, parameterNumber: 3, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: invert!=null? invert.toInteger() : 0, parameterNumber: 4, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 4)
    cmds << zwave.configurationV1.configurationSet(configurationValue: autoOff!=null? integer2Cmd(autoOff.toInteger(), 2) : integer2Cmd(0,2), parameterNumber: 5, size: 2)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 5)
    if (state.disableLocal != settings.disableLocal) {
        cmds << zwave.protectionV2.protectionSet(localProtectionState : disableLocal!=null? disableLocal.toInteger() : 0, rfProtectionState: 0)
        cmds << zwave.protectionV2.protectionGet()
    }
    
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
            if (logEnabled) log.debug("'$description' parsed to $result")
        } else {
            if (logEnabled) log.debug("Couldn't zwave.parse '$description'")
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
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x25: 1])
    if (encapsulatedCommand) {
        state.sec = 1
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "Unhandled: $cmd"
    null
}

def on() {
    if (buttonOn == "1") {
        sendEvent([name: "pushed", value: 1, isStateChange:true])
    }
    if (disableRemote != "2") {
        commands([
            zwave.basicV1.basicSet(value: 0xFF),
            zwave.basicV1.basicGet()
        ])
    } else {
        commands([
            zwave.basicV1.basicGet()
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
            zwave.basicV1.basicGet()
        ])
    } else {
        commands([
            zwave.basicV1.basicGet()
        ])
    }
}

def ping() {
    refresh()
}

def poll() {
    refresh()
}

def refresh() {
    commands(zwave.basicV1.basicGet())
}

def childSetLevel(String dni, value) {
    def valueaux = value as Integer
    def level = Math.max(Math.min(valueaux, 99), 0)    
    def cmds = []
    switch (channelNumber(dni)) {
        case 101:
            cmds << new hubitat.device.HubAction(command(zwave.protectionV2.protectionSet(localProtectionState : level > 0 ? 2 : 0, rfProtectionState: 0) ), hubitat.device.Protocol.ZWAVE)
            cmds << new hubitat.device.HubAction(command(zwave.protectionV2.protectionGet() ), hubitat.device.Protocol.ZWAVE)
        break
    }
    cmds
}

private channelNumber(String dni) {
    dni.split("-ep")[-1] as Integer
}

def childOn(String dni) {
    if (logEnabled) log.debug "childOn($dni)"
    childSetLevel(dni, 99)
}

def childOff(String dni) {
    if (logEnabled) log.debug "childOff($dni)"
    childSetLevel(dni, 0)
}

def childRefresh(String dni) {
    if (logEnabled) log.debug "childRefresh($dni)"
}

def componentSetLevel(cd,level,transitionTime = null) {
    if (infoEnable) log.info "${device.label?device.label:device.name}: componentSetLevel($cd, $value)"
        return childSetLevel(cd.deviceNetworkId,level)
}

def componentOn(cd) {
    if (infoEnable) log.info "${device.label?device.label:device.name}: componentOn($cd)"
    return childOn(cd.deviceNetworkId)
}

def componentOff(cd) {
    if (infoEnable) log.info "${device.label?device.label:device.name}: componentOff($cd)"
    return childOff(cd.deviceNetworkId)
}

void componentRefresh(cd) {
    if (infoEnable) log.info "${device.label?device.label:device.name}: componentRefresh($cd)"
}

def childExists(ep) {
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith(ep)}
    if (childDevice) 
        return true
    else
        return false
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
                if (logEnabled) log.debug "Adding node $it to group $i"
                cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:hubitat.helper.HexUtils.hexStringToInt(it))
                refreshGroup = true
            }
            ((state."actualAssociation${i}" - state."defaultG${i}") - state."desiredAssociation${i}").each {
                if (logEnabled) log.debug "Removing node $it from group $i"
                cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId:hubitat.helper.HexUtils.hexStringToInt(it))
                refreshGroup = true
            }
            if (refreshGroup == true) cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
            else if (logEnabled) log.debug "There are no association actions to complete for group $i"
         }
      } else {
         if (logEnabled) log.debug "Association info not known for group $i. Requesting info from device."
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
    if (logEnabled) log.debug "Associations for Group ${cmd.groupingIdentifier}: ${temp}"
    updateDataValue("associationGroup${cmd.groupingIdentifier}", "$temp")
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    sendEvent(name: "groups", value: cmd.supportedGroupings)
    if (logEnabled) log.debug "Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
    if (logEnabled) log.debug cmd
    if(cmd.applicationVersion && cmd.applicationSubVersion) {
        def firmware = "${cmd.applicationVersion}.${cmd.applicationSubVersion.toString().padLeft(2,'0')}"
        state.needfwUpdate = "false"
        createEvent(name: "firmware", value: "${firmware}")
    }
}

def zwaveEvent(hubitat.zwave.commands.protectionv2.ProtectionReport cmd) {
    if (logEnabled) log.debug cmd
    def integerValue = cmd.localProtectionState
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith("ep101")}
    if (childDevice) {
        childDevice.sendEvent(name: "switch", value: integerValue > 0 ? "on" : "off")        
    }
}

