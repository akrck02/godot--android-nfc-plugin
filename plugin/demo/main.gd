extends Control

var _plugin_name = "GodotNFC"
var _android_plugin

func _ready():
	print("--------- LOADING TEST ---------")
	if Engine.has_singleton(_plugin_name):
		_android_plugin = Engine.get_singleton(_plugin_name)
		_android_plugin.connect("nfc_enabled", _on_nfc_enabled)
		_android_plugin.connect("nfc_scanned", _on_nfc_tag_readed)
		_android_plugin.enableNfc()
		_android_plugin.setNfcCallback()
	else:
		printerr("Couldn't find plugin " + _plugin_name)
		

func _on_nfc_enabled(status : String) -> void:
	$Control/Label.text += "NFC Status:%s\n" % status


func _on_nfc_tag_readed(data : String) -> void:
	print(data)
	$Control/Label.text +=  "NFC Read Data:%s\n" % data
