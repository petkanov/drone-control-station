class WebSocketClient {
    
    constructor(droneId, hostname, port, endpoint) {
        this.droneId = droneId;
        this.webSocket = null;
        this.hostname = hostname;
        this.port     = port;
        this.endpoint = endpoint;
    }
    
    getServerUrl() {
        return "ws://"+this.hostname+":" + this.port + this.endpoint;
    }
    
    connect() {
        this.webSocket = new WebSocket( this.getServerUrl() );

        var activeDroneId = this.droneId;

        this.webSocket.onopen = function(event) {
            console.log('Requesting Video Stream For Drone ID ' + activeDroneId);
            this.send(activeDroneId);
        }
            
        this.webSocket.onmessage = function(event) {
		   var msg = "data:image/jpg;base64,"+event.data+"";
  		   $('#video'+activeDroneId).attr("src", msg);
        }
    }
    
    getStatus() {
        return this.webSocket.readyState;
    }
    
    send(message) {
        if (this.webSocket != null && this.webSocket.readyState == WebSocket.OPEN) {
            this.webSocket.send(message);
        } 
    }
    
    disconnect() {
        if (this.webSocket != null) {
            this.webSocket.close();
        } 
    }
}


      class Drone {
    	  constructor(id, name, lat, lng, map, webSocketURL){
    		  this.id = id;
    		  this.name = name;
    		  this.lat = lat;
    		  this.lng = lng;
    		  this.map = map;
    		  this.videoSocket = new WebSocketClient(id, localIp, 80, "/videofeed");
    		  this.posMark = new google.maps.Marker({position:{lat:lat, lng:lng}, 
    			                                     map:map, label:name+'',
    			                                     icon: 'drone.svg' });
    		  this.defaultSpeed = 5;
    		  this.defaultHeight = 3;
    		  this.locationToPointDataMap = new Map();
    		  this.currentLabel = 0;
    		  this.labels = ['1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21'];
    		  this.speed = 0.0;
    		  this.alt = 0.0
    	  }
    	  
    	  static createPointID(marker){
    		  return marker.getPosition().lat()+""+marker.getPosition().lng();
    	  }
    	  
    	  startVideoFeed(){
    		  this.videoSocket.disconnect();
    		  this.videoSocket.connect();
    	  }

    	  stopVideoFeed(){
    		  this.videoSocket.disconnect();
    	  }
    	  
    	  getId(){
    		  return this.id;
    	  }
    	  getName(){
    		  return this.name;
    	  }
    	  getSpeed(){
    		  return this.speed;
    	  }
    	  setSpeed(speed){
    		  this.speed = speed;
    	  }
    	  getLat(){
    		  return this.lat;
    	  }
    	  getLng(){
    		  return this.lng;
    	  }
    	  getAltitude(){
    		  return this.alt;
    	  }
    	  setAltitude(alt){
    		  this.alt = alt;
    	  }
    	  setPosition(lat, lng, alt){
    		  this.posMark.setPosition({lat:lat, lng:lng, alt:alt});
    		  this.lat = lat;
    		  this.lng = lng;
    		  this.alt = alt;
    	  }
    	  getNextLabelIndex(){
    		  return this.labels[this.currentLabel++ % this.labels.length];
    	  }
    	  addPoint(marker){
    		  var pointId = Drone.createPointID(marker);
    		  var pointData = new PointData(marker, this.defaultSpeed, this.defaultHeight);
    		  this.locationToPointDataMap.set(pointId, pointData);
    		  return pointId;
    	  }
    	  getPointDataForID(key){
    		  return this.locationToPointDataMap.get(key);
    	  }
    	  removePoint(key){
    		  this.locationToPointDataMap.get(key).getMarker().setMap(null);
    		  this.locationToPointDataMap.delete(key);
    	  }
    	  hidePoints(){
    		  this.locationToPointDataMap.forEach(function(pointData, key, map){
    			  pointData.getMarker().setMap(null);
    		  });
    	  }
    	  showPoints(){
    		  var m = this.map;
    		  this.locationToPointDataMap.forEach(function(pointData, key, map){
    			  pointData.getMarker().setMap(m);
    		  });
    	  }
    	  removePoints(){
    		  this.hidePoints();
    		  this.locationToPointDataMap = new Map();
    		  this.currentLabel = 0;
    	  }
    	  printPointsData(){
    		  this.locationToPointDataMap.forEach(function(pointData, key, map){
    			  console.log(key);
    			  console.log(pointData.getAction());
    			  console.log(pointData.getHeight());
    			  console.log(pointData.getSpeed());
    			  console.log(pointData.getMarker().getPosition().lat());
    			  console.log(pointData.getMarker().getPosition().lng());
    		  });
    	  }
    	  getPointDataJSON(){
    		  var result = '[';
    		  var droneId = this.id;
    		  this.locationToPointDataMap.forEach(function(pointData, key, map){
				  result +='{"lat":"'+pointData.getMarker().getPosition().lat()+'",' +
				            '"lng":"'+pointData.getMarker().getPosition().lng()+'",' +
				            '"speed":'+pointData.getSpeed()+',' +
				            '"height":'+pointData.getHeight()+',' +
				            '"action":'+pointData.getAction()+'},';
				  });
    		  return result.substring(0, result.length-1)+']';
    	  }
    	  startMission(){
    		  $.ajax({
    				type: 'POST',
    				url: '/startMission',
    				data: {points: this.getPointDataJSON(),
    					   droneId: this.id}
    			})
    			.done(function(response) {
						console.log(response)
    			})
    			.fail(function(data) {
						console.log(data)
    			});
    	  }
    	  sendCommand(commandId){
    		  
      		console.log(this.id+' -- '+commandId);
      		
      		$.ajax({
    				type: 'POST',
    				url: '/sendCommand',
    				data: {commandCode: commandId, droneId: this.id}
    			})
    			.done(function(response) {
  						
    			})
    			.fail(function(data) {

    			});
      	  }
      }
      
      class PointData {
    	  constructor(marker, speed, height){
    		  this.marker = marker;
    		  this.speed = speed;
    		  this.height = height;
    		  this.action = 0;
    	  }
    	  getMarker(){
    		  return this.marker;
    	  }
    	  getSpeed(){
    		  return this.speed;
    	  }
    	  setSpeed(value){
    		  this.speed = value;
    	  }
    	  getHeight(){
    		  return this.height;
    	  }
    	  setHeight(value){
    		  this.height = value;
    	  }
    	  getAction(){
    		  return this.action;
    	  }
    	  setAction(value){
    		  this.action = value;
    	  }
      }
      
      var CommandType = {
    		  START_MISSION: 14,
    		  CANCEL_MISSION : 6,
    		  FORWARD : 11,
    		  MLEFT : 15,
    		  MRIGHT : 16,
    		  CANCEL_XMOVE : 12,
    		  CANCEL_ZMOVE : 13,
    		  UP : 1,
    		  RLEFT : 2,
    		  RRIGHT : 3,
    		  BACKWARD : 4,
    		  DOWN : 5,
    		  RETURN_TO_LAUNCH : 7,
    		  ACTIVATE_FUNCTION : 8,
    		  ARM : 9,
    		  KILL : 17,
    		  DISARM : 10,
    		  RLEFT45 : 18,
    		  RLEFT90 : 19,
    		  RRIGHT45 : 20,
    		  RRIGHT90 : 21,
      }




      
      
      var dronesCount = 0;
      var dronesAll = new Map();
      
      var activeDrone;
      
      var droneSpeed = 5;
      var droneHeight = 3;
      
      var mapCenter = { lat: 42.69, lng: 23.31 };
      
      var map;
      


      google.maps.event.addDomListener(window, 'load', initializeApp);
      
      function initializeApp() {
    	  
        map = new google.maps.Map(document.getElementById('map'), {
          zoom: 2,
          center: mapCenter
        });
       
        google.maps.event.addListener(map, 'click', function(event) {
          addMarker(event.latLng, map);
        });
        
        setInterval( updateSystemData, 1000);
      }
      
      function addMarker(location, map) {
    	  if (activeDrone == null || activeDrone == undefined){
    		  return;
    	  }
    	  
          var marker = new google.maps.Marker({
            position: location,
            draggable: true,
            label: activeDrone.getNextLabelIndex(),
            map: map
          });
        
          var pointId = activeDrone.addPoint(marker);
        
          var contentString = '<form id="'+pointId+'">'+
        	'<input type="text" name="height" value="'+droneHeight+'"/> Height' + '<br />' +
        	'<input type="text" name="speed" value="'+droneSpeed+'" /> Speed' + '<br />' +
        	'<input type="hidden" name="key" value="'+pointId+'"/>' +
       		'<select name="action"><option value="0">No Action</option><option value="1">Activate</option></select>' + '<br />' +
        	'<input type="submit" value="Save" onClick="updatePointValue(this.form); return false;" />'+
        	'<input type="button" value="Remove" onClick="removePoint(this.form); return false;" />' + '<br />' +
          '</form>';
          
          var infowindow = new google.maps.InfoWindow({
              content: contentString
          });
        
          marker.addListener('click', function(event) {
              infowindow.open(map, marker);
          });

          marker.addListener('dragend', function(event) {
        	  marker.setPosition(event.latLng);
          });
      }
      
      var updatePointValue = function(form){
    	  var pointData = activeDrone.getPointDataForID(form["key"].value);
    	  pointData.setSpeed(form["speed"].value);
    	  pointData.setHeight(form["height"].value);
    	  pointData.setAction(form["action"].value);
      }

      var removePoint = function(form){
    	  activeDrone.removePoint(form["key"].value);
      }

      
      var updateSystemData = function () {
    	  $.ajax({
  			type: 'GET',
  			url: '/updateSystemInfo',
  		  })
  		  .done(function(response) {
  			  
  			  loadDronesData(map, dronesAll, response);

  			  if(activeDrone != undefined){
  				  map.setCenter({ lat:activeDrone.getLat(), lng:activeDrone.getLng() });
  		      }
  			  
  		  })
  		  .fail(function(data) {
  			    loadDronesData(map, dronesAll, '[{}]');
  		  });
      }
      
      
      
      var loadDronesData = function (map, dronesMap, data) {
    	  var dronesDTOs = JSON.parse(data);
    	  
    	  $("p[id*='onlineStatus']").html('OFFLINE');
    	  
    	  dronesDTOs.forEach(function(droneDTO, index){
    		  
    		  if(droneDTO == undefined || droneDTO.id == undefined){
    			  return;
    		  }
    		  
    		  if(dronesMap.has(droneDTO.id)){
    		     $('#onlineStatus'+droneDTO.id).html('ONLINE'); 
    		     $('#armedStatus'+droneDTO.id).html(droneDTO.state);
    		     
     			 var drone = dronesMap.get(droneDTO.id);
     			 drone.setPosition(droneDTO.lattitude, droneDTO.longitude, droneDTO.alt);
     			 
    		     $('#infoAlt'+droneDTO.id).val(droneDTO.alt); 
    		     $('#infoSpeed'+droneDTO.id).val(droneDTO.speed); 
    		     $('#infoBat'+droneDTO.id).val(droneDTO.battery); 
    		  } 
    		  else{
     			 var drone = new Drone(droneDTO.id, droneDTO.name, droneDTO.lattitude, droneDTO.longitude, map);
    		     drone.setSpeed(droneDTO.speed);
    		     drone.setAltitude(droneDTO.alt);
    		  
    		     dronesMap.set(drone.getId(), drone);
    		  // width="770" height="601"
    		     $('.dronesList').append('<div droneId="'+drone.getId()+'" class="dronesList-header" id="ctrlPanel1">Drone: '+drone.getName()+
    	  			      
    	  			      '&nbsp;&nbsp; > &nbsp;&nbsp; <label>Altitude (m)&nbsp;</label><input type="text" id="infoAlt'+drone.getId()+'" size="2" value="'+droneDTO.alt+'" disabled />' +
    	  			      ' &nbsp; <label>Speed (km/h)&nbsp;</label><input type="text" id="infoSpeed'+drone.getId()+'" size="2" value="'+droneDTO.speed+'" disabled />' +
    	  			      ' &nbsp; <label>Voltage&nbsp;</label><input type="text" id="infoBat'+drone.getId()+'" size="2" value="'+droneDTO.battery+'" disabled />' +
    	  			      
    	    		    '<p id="onlineStatus'+drone.getId()+'" class="drone-status">ONLINE</p>'+
    	    		    '<p id="armedStatus'+drone.getId()+'" class="drone-arm-status">'+(droneDTO.state)+'</p></div>'+
    	    		    
    	  			    '<div class="dronesList-content" style="position:relative;">' + 
    	  			    
    	  			      '<img id="video'+drone.getId()+'" src="video.jpg" style="width: 100%;"  onclick="dronesAll.get(\''+droneDTO.id+'\').startVideoFeed(); set_FPV_active();" > <br />' +
    	  			    
    	  			      
    	  				'<div id="ctrlPanel2" style="position: absolute; top: 56%; float: left;">'+
    	  				'<table><tr><td> </td>' +
    	  				'<td> <input class="button" id="btnF'+drone.getId()+'" type="button" value="FORWARD" /></td>'+
    	  				'<td> </td> </tr>' +
    	  				'<tr> <td><input class="button" id="btnMvL'+drone.getId()+'" type="button" value=" LEFT " /> </td>' +
    	  				'<td> <input class="button" id="btnCncl'+drone.getId()+'" type="button" value=" STOP " /></td>'+
    	  				'<td> <input class="button" id="btnMvR'+drone.getId()+'" type="button" value=" RIGHT " /> </td></tr>' +
    	  				'<tr> <td> </td>' +
    	  				'<td><input class="button" id="btnB'+drone.getId()+'" type="button" value="BACKWARD" /></td>' +
    	  				'<td> </td></tr></table></div>' +


    	  			      
    	  				'<div id="ctrlPanel3" style="position: absolute; top: 35%;left:50px;">'+
    	  				'<table><tr> <td> <input class="button" id="mStart'+drone.getId()+'" type="button" value="START/PAUSE Mission" /> </td></tr> '+
    	  				'<tr> <td> <input class="button" id="mCancel'+drone.getId()+'" type="button" value="CLEAR Mission Data" /> </td></tr>' +
    	  				'</table></div>' + 


    	  			      
    	  				'<div id="ctrlPanel4" style="position: absolute; top: 56%;right:30px;">'+ 
    	  				'<table><tr> <td> <input class="button" id="btnRLEFT45'+drone.getId()+'" type="button" value="LEFT45" /> </td>' +
    	  				'<td> <input class="button" id="btnU'+drone.getId()+'" type="button" value="UPWARD" />'+'</td> '+
    	  				'<td> <input class="button" id="btnRRIGHT45'+drone.getId()+'" type="button" value="RIGHT45" /></td></tr>' + 
    	  				'<tr> <td> <input class="button" id="btnRL'+drone.getId()+'" type="button" value="ROTATE-L" /></td>' +
    	  				'<td> <input class="button" id="btnStopZ'+drone.getId()+'" type="button" value="STOP" /></td> ' +
    	  				'<td> <input class="button" id="btnRR'+drone.getId()+'" type="button" value="ROTATE-R" /></td> </tr>' + 
    	  				'<tr> <td> <input class="button" id="btnRLEFT90'+drone.getId()+'" type="button" value="LEFT90" /> </td>' +
    	  				'<td> <input class="button" id="btnD'+drone.getId()+'" type="button" value="DOWN" /> </td> '+
    	  				'<td> <input class="button" id="btnRRIGHT90'+drone.getId()+'" type="button" value="RIGHT90" /> </td></tr>' +
    	  				'</table></div>' +

    	  				
    	  				
    	  				'<div id="ctrlPanel5" style="position:relative;top:-50px;">' + 
    	  				'<input class="button" id="fArm'+drone.getId()+'" type="button" value="TAKEOFF" style="width:16%;float:left;" />' +
    	  				'<input class="button" id="fDisarm'+drone.getId()+'" type="button" value="LAND"  style="width:16%;float:left;margin-left:15px;"/>' +
    	  				'<input class="button" id="mRTL'+drone.getId()+'" type="button" value="RETURN HOME"  style="width:16%;float:left;margin-left:15px;" />' +
    	  				'<input class="button" id="fActivate'+drone.getId()+'" type="button" style="width:16%;margin-left:65px;float:left;" value="DROP PACKAGE"/>' +
    	  				'<input class="button" onclick="copyToClipboard(\'copyLink'+droneDTO.id+'\')" type="button" style="width:16%;float:right;" value="SHARE Video Feed"/>' +
    	  				'<input type="text" size="34" style="position:relative;float:right;"  id="copyLink'+drone.getId()+'" value="http://'+localIp+'/v/'+droneDTO.id+'" />' +
    	  				'</div>'+
    	  			    
    	  			    
    	  			    '<div id="ctrlPanel6" style="position: absolute; top: 30px;left:30px;">' + 
    	  			    '<input class="button" onclick="set_Map_active()" type="button" value="VIEW ON MAP" style="width:148px;background-color:green;opacity:0.55;"/>' +
    	  			    '</div>'+ 
    	  			    
    	  			    '<div id="ctrlPanel7" style="position: absolute; top: 30px;right:30px;">' + 
    	  			    '<input class="button" id="fKill'+drone.getId()+'" type="button" value="KILL MOTORS"  style="width:133px;background-color:red;opacity:0.55;"/>' +
    	  			    '</div>'+ 
    	  			    
    	  			    '</div>');
    		     
    		     $(".dronesList").on("click", ".dronesList-header", function () {
    		        	map.setZoom(18);

    		        	set_FPV_active();
    		        	
    		        	if($(this).hasClass("active")){
//    		        		client.disconnect();
    		        	     var drone = dronesAll.get( $(this).attr('droneId'), 10);
    		        	     drone.stopVideoFeed();
    		        		return;
    		        	}
    		        	$(".dronesList > .active").each(function( index ) {
    		        	     $(this).removeClass("active").next().slideToggle();
    		        	     var drone = dronesAll.get( $(this).attr('droneId'), 10);
    		        	     drone.stopVideoFeed();
    		    		     drone.hidePoints();
    		      	    });
    		        	
    		        	$(this).toggleClass("active").next().slideToggle();
    		        	activeDrone = dronesAll.get( $(this).attr('droneId'), 10);
    		        	activeDrone.showPoints();
    		        	
    		        });
    		     
    		     initializeDronesControls(drone.getId());
    		  }
		  });
      }
      

      var set_FPV_active = function() { 
    	  $('#map').hide();
    	  
    	  $('.dronesList').css({"width": "100%" });
    	  $('#ctrlPanel2').css({"position":"absolute", "top":"56%", "float":"left", "margin-top":"", "left":"", "width":"" });
    	  $('#ctrlPanel3').css({"position":"absolute", "top":"35%", "left":"50px", "margin-top":"","float":"", "left":"", "width":"" });
    	  $('#ctrlPanel4').css({"position":"absolute", "top":"56%", "right":"30px", "margin-top":"","float":"", "left":"", "width":"" });
    	  
    	  $('div[id^="ctrlPanel"]').show();
      }
      
      var set_Map_active = function() {
    	    $('#map').show();
    	    $('#map').css({"width": "65%", "height":"950px", "position":"relative", "float":"left", "top":"0px", "left":"0px", "opacity":"1" });
    	    
 	        $('#ctrlPanel1').hide();
 	        $('#ctrlPanel5').hide();
 	        $('#ctrlPanel6').hide();
 	        $('#ctrlPanel7').hide();
 	        
 	        $('#ctrlPanel2').css({"position":"relative","float":"left","margin-top":"25px", "left":"-25px", "width":"100%","right":"","top":"" });
 	        $('#ctrlPanel3').css({"position":"relative","float":"left","margin-top":"25px", "left":"-25px", "width":"100%","right":"","top":"" });
    	    $('#ctrlPanel4').css({"position":"relative","float":"left","margin-top":"25px", "left":"-25px", "width":"100%","right":"","top":"" });
    	    
    	    $('.dronesList').css({"width": "35%" });
      }
      
      
      var initializeDronesControls = function (id) {
          $("input[id='mStart"+id+"']").click(function() {
          	activeDrone.startMission();
          });
          $("input[id*='mCancel"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.CANCEL_MISSION);
          	activeDrone.removePoints();
          });
          $("input[id*='mRTL"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.RETURN_TO_LAUNCH);
          });
          $("input[id*='fActivate"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.ACTIVATE_FUNCTION);
          });
          $("input[id*='fArm"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.ARM);
          });
          $("input[id*='fDisarm"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.DISARM);
          });
          $("input[id*='fKill"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.KILL);
          });

          $("input[id*='btnF"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.FORWARD);
          });

          $("input[id*='btnMvL"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.MLEFT);
          });

          $("input[id*='btnMvR"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.MRIGHT);
          });
          
          $("input[id*='btnCncl"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.CANCEL_XMOVE);
          });
          
          $("input[id*='btnB"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.BACKWARD);
          });
          
          $("input[id*='btnU"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.UP);
          });

          $("input[id*='btnStopZ"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.CANCEL_ZMOVE);
          });
          
          $("input[id*='btnD"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.DOWN);
          });
          
          $("input[id*='btnRL"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.RLEFT);
          });
          
          $("input[id*='btnRR"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.RRIGHT);
          });
          
          $("input[id*='btnRLEFT45"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.RLEFT45);
          });
          
          $("input[id*='btnRLEFT90"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.RLEFT90);
          });
          
          $("input[id*='btnRRIGHT45"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.RRIGHT45);
          });
          
          $("input[id*='btnRRIGHT90"+id+"']").click(function() {
          	activeDrone.sendCommand(CommandType.RRIGHT90);
          });
      }


      
      var copyToClipboard = function(elmId) {
    	  var copyLink = document.getElementById(elmId);
    	  copyLink.select();
    	  copyLink.setSelectionRange(0, 99999);
    	  
    	  document.execCommand("copy");
      }

