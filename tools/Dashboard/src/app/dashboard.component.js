(function () {
  'use strict';

  angular.module('dashboardFramework')
    .component('dashboard', {
      templateUrl: 'app/dashboard.html',
      controller: MainController,
      controllerAs: 'vm',
      bindings:{
        editmode : "=",
        iframe : "=",
        selectedpage : "&",
        id: "@",
        public: "=",
        synop: "="
      }
    });

  /** @ngInject */
  function MainController($log, $rootScope, $scope, $mdSidenav, $mdDialog, $timeout, $window, httpService, interactionService,urlParamService, gadgetManagerService,filterService) {
    var vm = this;
    vm.$onInit = function () {
     vm.showSynopticEditor = false;
      if(vm.editmode){
        vm.showSynopticEditor=true;
      }
      vm.selectedpage = 0;
      vm.synopticEdit = {
        zindexEditor:600,
        showEditor:vm.showSynopticEditor,
        showSynoptic: vm.synop
      }
    

      // pdf configuration
      var margins = {
        top: 70,
        bottom: 40,
        left: 30,
        width: 550
      };

      vm.margins = margins;

      $rootScope.dashboard = angular.copy(vm.id);

      /*Rest api call to get dashboard data*/
      httpService.getDashboardModel(vm.id).then(
        function(model){
          vm.dashboard = model.data;

          vm.dashboard.gridOptions.resizable.stop = sendResizeToGadget;

          vm.dashboard.gridOptions.enableEmptyCellDrop = true;
          if(!vm.iframe){
            vm.dashboard.gridOptions.emptyCellDropCallback = dropElementEvent.bind(this); 
          } 
          //If interaction hash then recover connections
          if(vm.dashboard.interactionHash){
            interactionService.setInteractionHash(vm.dashboard.interactionHash);
          }
           //If interaction hash then recover connections
           if(vm.dashboard.parameterHash){
            urlParamService.seturlParamHash(vm.dashboard.parameterHash);
          }
          vm.dashboard.gridOptions.displayGrid = "onDrag&Resize";
          if(!vm.editmode){           
            vm.dashboard.gridOptions.draggable.enabled = false;
            vm.dashboard.gridOptions.resizable.enabled = false;
            vm.dashboard.gridOptions.enableEmptyCellDrop = false;
			      vm.dashboard.gridOptions.displayGrid = "none";
          }
          gadgetManagerService.setDashboardModelAndPage(vm.dashboard,vm.selectedpage);
          if(!vm.editmode){ 
            var urlParamMandatory = urlParamService.checkParameterMandatory();
            if(urlParamMandatory.length>0){
              showUrlParamDialog(urlParamMandatory);
          }
        }
        }
      ).catch(
        function(){
          $window.location.href = "/controlpanel/login";
        }
      )

      function addGadgetHtml5(type,config,layergrid){
        addGadgetGeneric(type,config,layergrid);
      } 
      function addGadgetFilter(type,config,layergrid){
        addGadgetGeneric(type,config,layergrid);
      } 


      function addGadgetGeneric(type,config,layergrid){
        config.type = type;
        layergrid.push(config);
        $timeout(
         function(){
           $scope.$broadcast("$resize", "");
         },100
       );
       } 

      
      vm.api={};
      //External API
      vm.api.createGadget = function(type,id,name,template,datasource,filters,setupLayout) {
            if(typeof template !== "undefined" && template !== null && typeof template !=="string"  ){
              //Gadgetcreate from template
              var newElem = {x: 0, y: 0, cols: 6, rows: 6,};
              //newElem.minItemRows = 10;
              //newElem.minItemCols = 10;            
              newElem.content=template.template;        
              newElem.contentcode = template.templateJS;
              newElem.id = id;             
              newElem.type = 'livehtml';
              newElem.idtemplate =  template.identification;
              newElem.header = {
                enable: true,
                title: {
                  iconColor: "hsl( 206, 54%, 5%)",
                  text: name, 
                  textColor: "hsl( 206, 54%, 5%)"
                },
                backgroundColor: "hsl(0, 0%, 100%)",
                height: 40
              } 
              newElem.backgroundColor ="white";
              newElem.padding = 0;
              newElem.border = {
                color: "hsl(0, 0%, 90%)",
                width: 0,
                radius: 5
              }  
              newElem.datasource = datasource;
              newElem.filters = filters;  
              newElem.filtersInModal=setupLayout.filtersInModal;
              newElem.hideBadges=setupLayout.hideBadges;
              newElem.hidebuttonclear=setupLayout.hidebuttonclear;
              addGadgetGeneric(type,newElem,vm.dashboard.pages[vm.selectedpage].layers[vm.dashboard.pages[vm.selectedpage].selectedlayer].gridboard);        

            }else{
              var newElem = {x: 0, y: 0, cols: 6, rows: 6,};
              //newElem.minItemRows = 10;
              //newElem.minItemCols = 10;
              var type = type;
              newElem.id = id;
              newElem.content = type;
              newElem.type = type;
              newElem.idtemplate = type;
              newElem.header = {
                enable: true,
                title: {
                  iconColor: "hsl( 206, 54%, 5%)",
                  text: name, 
                  textColor: "hsl( 206, 54%, 5%)"
                },
                backgroundColor: "hsl(0, 0%, 100%)",
                height: 40
              }
              newElem.backgroundColor ="white";
              newElem.padding = 0;
              newElem.border = {
                color: "hsl(0, 0%, 90%)",
                width: 0,
                radius: 5
              }    
              newElem.filters = filters;    
              newElem.filtersInModal = setupLayout.filtersInModal;
              newElem.hideBadges = setupLayout.hideBadges;
              newElem.hidebuttonclear = setupLayout.hidebuttonclear;      
              addGadgetGeneric(type,newElem,vm.dashboard.pages[vm.selectedpage].layers[vm.dashboard.pages[vm.selectedpage].selectedlayer].gridboard);        

            }
         
      };

      vm.api.updateFilterGadget = function(id,filters,merge) {
        $scope.$apply(function() {    
        var gadgets = vm.dashboard.pages[vm.selectedpage].layers[vm.dashboard.pages[vm.selectedpage].selectedlayer].gridboard;
        for(var i=0;i<gadgets.length;i++){
          var gadget = gadgets[i];       	
          if(typeof gadget.id!=="undefined" && gadget.id === id){
            if(typeof gadget.filters!=="undefined" && gadget.filters!==null ){
              for(var j=0;j<gadget.filters.length;j++){
                var filter = gadget.filters[j];         
                for(var k=0;k<filters.length;k++){
                  if(filter.id === filters[k].id){
                      if(filters[k].type === 'multiselectfilter' || filters[k].type === 'multiselectnumberfilter'){
                        if(merge){
                          filter.data.options = Array.from(new Set(filter.data.options.concat(filters[k].data.options)));
                          filter.data.optionsSelected = Array.from(new Set(filter.data.optionsSelected.concat(filters[k].data.optionsSelected)));
                        }else{
                          filter.data.options = filters[k].data.options;
                          filter.data.optionsSelected = filters[k].data.optionsSelected;
                        }
                      } else if(filters[k].type === 'textfilter'){
                        filter.value = filters[k].value;
                      } else if(filters[k].type === 'numberfilter'){
                        filter.value = filters[k].value;
                      }
                       
                    }
                  }
                }		
              }              
              angular.element(document.querySelector('#'+id)).controller("element").config = gadget.filters;
              filterService.sendFilters(id,gadget.filters);
              break;
            }
          }    }) 
        
      }


      
      vm.api.dropOnElement = function(x,y) {
        if(x !=null && y !=null){
        vm.dashboard.pages[vm.selectedpage].layers[vm.dashboard.pages[vm.selectedpage].selectedlayer].gridboard;
        var elements = document.getElementsByTagName("element");
        if(elements!=null && elements.length>0){
          for (var index = 0; index < elements.length; index++) {
            var element = elements[index];
            console.log(element.firstElementChild.getBoundingClientRect());
            var sl = element.firstElementChild.getBoundingClientRect().x;
            var sr = element.firstElementChild.getBoundingClientRect().x+ element.firstElementChild.getBoundingClientRect().width;
            var st = element.firstElementChild.getBoundingClientRect().y;
            var sb = element.firstElementChild.getBoundingClientRect().y + element.firstElementChild.getBoundingClientRect().height;
            if(x>=sl && x<=sr && y<= sb && y >= st){
              return {"dropOnElement":"TRUE","idGadget":element.id,"type":element.getAttribute('idtemplate')};
            }
          }
        }
      }
        return {"dropOnElement":"FALSE","idGadget":"","type":""};
      }

      vm.api.refreshGadgets = function(){
        var gadgets =document.querySelectorAll( 'gadget' ) ;
        if(gadgets.length>0){
         for (var index = 0; index < gadgets.length; index++) {
           var gad = gadgets[index];
           angular.element(gad).scope().$$childHead.reloadContent();
         }        
       }


      }

      //END External API




      function showAddGadgetDialog(type,config,layergrid){
        function AddGadgetController($scope,__env, $mdDialog, httpService, type, config, layergrid) {
          $scope.type = type;
          $scope.config = config;
          $scope.layergrid = layergrid;

          $scope.gadgets = [];
         

          $scope.hide = function() {
            $mdDialog.hide();
          };

          $scope.cancel = function() {
            $mdDialog.cancel();
          };


          $scope.loadGadgets = function() {
            return httpService.getUserGadgetsByType($scope.type).then(
              function(gadgets){
                $scope.gadgets = gadgets.data;
              }
            );
          };

          $scope.addGadget = function() {
            $scope.config.type = $scope.type;
            $scope.config.id = $scope.gadget.id;
            $scope.config.header.title.text = $scope.gadget.identification;
            $scope.layergrid.push($scope.config);
            $mdDialog.cancel();
          };

          $scope.alert;
          $scope.newGadget = function($event) {
            var parentEl = angular.element(document.body);
            $mdDialog.show({
              parent: parentEl,
              targetEvent: $event,
              fullscreen: false,
              template:
                '<md-dialog id="dialogCreateGadget"  aria-label="List dialog">' +
                '  <md-dialog-content >'+
                '<iframe id="iframeCreateGadget" style=" height: 80vh; width: 80vw;" frameborder="0" src="'+__env.endpointControlPanel+'/gadgets/createiframe/'+$scope.type+'"+></iframe>'+                     
                '  </md-dialog-content>' +             
                '</md-dialog>',
              locals: {
                config:  $scope.config, 
                layergrid: $scope.layergrid,
                type: $scope.type
              },
              controller: DialogController
           });
           function DialogController($scope, $mdDialog, config, layergrid, type) {
             $scope.config = config;
             $scope.layergrid = layergrid;
             $scope.closeDialog = function() {
               
               $mdDialog.hide();
             }

              $scope.addGadgetFromIframe = function(type,id,identification) {
              $scope.config.type = type;
              $scope.config.id = id;
              $scope.config.header.title.text = identification;
              $scope.layergrid.push($scope.config);
              $mdDialog.cancel();
            };


           }
                };



        }

        $mdDialog.show({
          controller: AddGadgetController,
          templateUrl: 'app/partials/edit/addGadgetDialog.html',
          parent: angular.element(document.body),
          clickOutsideToClose:true,
          fullscreen: false, // Only for -xs, -sm breakpoints.
          openFrom: '.sidenav-fab',
          closeTo: angular.element(document.querySelector('.sidenav-fab')),
          locals: {
            type: type,
            config: config,
            layergrid: layergrid
          }
        })
        .then(function() {

        }, function() {
          $scope.status = 'You cancelled the dialog.';
        });
      }


      function showAddGadgetTemplateDialog(type,config,layergrid){
        function AddGadgetController($scope,__env, $mdDialog, httpService, type, config, layergrid) {
          $scope.type = type;
          $scope.config = config;
          $scope.layergrid = layergrid;

         
          $scope.templates = [];

          $scope.hide = function() {
            $mdDialog.hide();
          };

          $scope.cancel = function() {
            $mdDialog.cancel();
          };

         
          $scope.loadTemplates = function() {
            return httpService.getUserGadgetTemplate().then(
              function(templates){
                $scope.templates = templates.data;
              }
            );
          };

          $scope.useTemplate = function() {    
                 
            $scope.config.type = $scope.type;
            $scope.config.content=$scope.template.template        
            $scope.config.contentcode=$scope.template.templateJS
            showAddGadgetTemplateParameterDialog($scope.type,$scope.config,$scope.layergrid);
            $mdDialog.hide();
          };
          $scope.noUseTemplate = function() {
            $scope.config.type = $scope.type;        
            $scope.layergrid.push($scope.config);
            $mdDialog.cancel();
          };

        }
        $mdDialog.show({
          controller: AddGadgetController,
          templateUrl: 'app/partials/edit/addGadgetTemplateDialog.html',
          parent: angular.element(document.body),
          clickOutsideToClose:true,
          fullscreen: false, // Only for -xs, -sm breakpoints.
          openFrom: '.sidenav-fab',
          closeTo: angular.element(document.querySelector('.sidenav-fab')),
          locals: {
            type: type,
            config: config,
            layergrid: layergrid
          }
        })
        .then(function() {
       
        }, function() {
          $scope.status = 'You cancelled the dialog.';
        });
      }



      function showAddGadgetTemplateParameterDialog(type,config,layergrid){
        function AddGadgetController($scope,__env, $mdDialog,$mdCompiler, httpService, type, config, layergrid) {
          var agc = this;
          agc.$onInit = function () {
            $scope.loadDatasources();
            $scope.getPredefinedParameters();
          }
         
          $scope.type = type;
          $scope.config = config;
          $scope.layergrid = layergrid;
          $scope.datasource;
          $scope.datasources = [];
          $scope.datasourceFields = [];
          $scope.parameters = [];
         
          $scope.templates = [];

          $scope.hide = function() {
            $mdDialog.hide();
          };

          $scope.cancel = function() {
            $mdDialog.cancel();
          };

         
          $scope.loadDatasources = function(){
            return httpService.getDatasources().then(
              function(response){
                $scope.datasources=response.data;
                
              },
              function(e){
                console.log("Error getting datasources: " +  JSON.stringify(e))
              }
            );
          };
    
          $scope.iterate=  function (obj, stack, fields) {
            for (var property in obj) {
                 if (obj.hasOwnProperty(property)) {
                     if (typeof obj[property] == "object") {
                      $scope.iterate(obj[property], stack + (stack==""?'':'.') + property, fields);
              } else {
                         fields.push({field:stack + (stack==""?'':'.') + property, type:typeof obj[property]});
                     }
                 }
              }    
              return fields;
           }

          /**method that finds the tags in the given text*/
          function searchTag(regex,str){
            var m;
            var found=[];
            while ((m = regex.exec(str)) !== null) {  
                if (m.index === regex.lastIndex) {
                    regex.lastIndex++;
                }
                m.forEach(function(item, index, arr){			
                found.push(arr[0]);			
              });  
            }
            return found;
          }
          /**method that finds the name attribute and returns its value in the given tag */
          function searchTagContentName(regex,str){
            var m;
            var content;
            while ((m = regex.exec(str)) !== null) {  
                if (m.index === regex.lastIndex) {
                    regex.lastIndex++;
                }
                m.forEach(function(item, index, arr){			
                  content = arr[0].match(/"([^"]+)"/)[1];			
              });  
            }
            return content;
          }
          /**method that finds the options attribute and returns its values in the given tag */
          function searchTagContentOptions(regex,str){
            var m;
            var content=" ";
            while ((m = regex.exec(str)) !== null) {  
                if (m.index === regex.lastIndex) {
                    regex.lastIndex++;
                }
                m.forEach(function(item, index, arr){			
                  content = arr[0].match(/"([^"]+)"/)[1];			
              });  
            }
          
            return  content.split(',');
          }

          /**we look for the parameters in the source code to create the form */
          $scope.getPredefinedParameters = function(){
            var str =  $scope.config.content;
           	var regexTag =  /<![\-\-\s\w\>\=\"\'\,\:\+\_\/]*\-->/g;
		        var regexName = /name\s*=\s*\"[\s\w\>\=\-\'\+\_\/]*\s*\"/g;
            var regexOptions = /options\s*=\s*\"[\s\w\>\=\-\'\:\,\+\_\/]*\s*\"/g;
		        var found=[];
            found = searchTag(regexTag,str);	
            
            Array.prototype.unique=function unique (a){
              return function(){return this.filter(a)}}(function(a,b,c){return c.indexOf(a,b+1)<0
             }); 
            found = found.unique(); 
        
            for (var i = 0; i < found.length; i++) {			
              var tag = found[i];
              if(tag.replace(/\s/g, '').search('type="text"')>=0 && tag.replace(/\s/g, '').search('label-osp')>=0){	
                $scope.parameters.push({label:searchTagContentName(regexName,tag),value:"parameterTextLabel", type:"labelsText"});
              }else if(tag.replace(/\s/g, '').search('type="number"')>=0 && tag.replace(/\s/g, '').search('label-osp')>=0){
                $scope.parameters.push({label:searchTagContentName(regexName,tag),value:0, type:"labelsNumber"});              
              }else if(tag.replace(/\s/g, '').search('type="ds"')>=0 && tag.replace(/\s/g, '').search('label-osp')>=0){
                $scope.parameters.push({label:searchTagContentName(regexName,tag),value:"parameterDsLabel", type:"labelsds"});               
              }else if(tag.replace(/\s/g, '').search('type="ds_parameter"')>=0 && tag.replace(/\s/g, '').search('label-osp')>=0){
                $scope.parameters.push({label:searchTagContentName(regexName,tag),value:"parameterNameDsLabel", type:"labelsdspropertie"});               
              }else if(tag.replace(/\s/g, '').search('type="ds"')>=0 && tag.replace(/\s/g, '').search('select-osp')>=0){
                var optionsValue = searchTagContentOptions(regexOptions,tag); 
                $scope.parameters.push({label:searchTagContentName(regexName,tag),value:"parameterSelectLabel",type:"selects", optionsValue:optionsValue});	              
              }
             } 
            }
        

            /**find a value for a given parameter */
            function findValueForParameter(label){
                for (var index = 0; index <  $scope.parameters.length; index++) {
                  var element =  $scope.parameters[index];
                  if(element.label===label){
                    return element.value;
                  }
                }
            }
        
            /**Parse the parameter of the data source so that it has array coding*/
            function parseArrayPosition(str){
              var regex = /\.[\d]+/g;
              var m;              
              while ((m = regex.exec(str)) !== null) {                
                  if (m.index === regex.lastIndex) {
                      regex.lastIndex++;
                  } 
                  m.forEach( function(item, index, arr){             
                    var index = arr[0].substring(1,arr[0].length)
                    var result =  "["+index+"]";
                    str = str.replace(arr[0],result) ;
                  });
              }
              return str;
            }

            /** this function Replace parameteres for his selected values*/
            function parseProperties(){
              var str =  $scope.config.content;
              var regexTag =  /<![\-\-\s\w\>\=\"\'\,\:\+\_\/]*\-->/g;
              var regexName = /name\s*=\s*\"[\s\w\>\=\-\'\+\_\/]*\s*\"/g;
              var regexOptions = /options\s*=\s*\"[\s\w\>\=\-\'\:\,\+\_\/]*\s*\"/g;
              var found=[];
              found = searchTag(regexTag,str);	
          
              var parserList=[];
              for (var i = 0; i < found.length; i++) {
                var tag = found[i];			
               
                if(tag.replace(/\s/g, '').search('type="text"')>=0 && tag.replace(/\s/g, '').search('label-osp')>=0){                 
                  parserList.push({tag:tag,value:findValueForParameter(searchTagContentName(regexName,tag))});   
                }else if(tag.replace(/\s/g, '').search('type="number"')>=0 && tag.replace(/\s/g, '').search('label-osp')>=0){
                  parserList.push({tag:tag,value:findValueForParameter(searchTagContentName(regexName,tag))});   
                }else if(tag.replace(/\s/g, '').search('type="ds"')>=0 && tag.replace(/\s/g, '').search('label-osp')>=0){                
                  var field = parseArrayPosition(findValueForParameter(searchTagContentName(regexName,tag)).field);                               
                  parserList.push({tag:tag,value:"{{ds[0]."+field+"}}"});        
                }else if(tag.replace(/\s/g, '').search('type="ds_parameter"')>=0 && tag.replace(/\s/g, '').search('label-osp')>=0){                
                  var field = parseArrayPosition(findValueForParameter(searchTagContentName(regexName,tag)).field);                               
                  parserList.push({tag:tag,value:field});        
                }else if(tag.replace(/\s/g, '').search('type="ds"')>=0 && tag.replace(/\s/g, '').search('select-osp')>=0){                
                  parserList.push({tag:tag,value:findValueForParameter(searchTagContentName(regexName,tag))});  
                }
              } 
              //Replace parameteres for his values
              for (var i = 0; i < parserList.length; i++) {
                str = str.replace(parserList[i].tag,parserList[i].value);
              }
              return str;
            }
          
          



      
          $scope.loadDatasourcesFields = function(){
            
            if($scope.config.datasource!=null && $scope.config.datasource.id!=null && $scope.config.datasource.id!=""){
                 return httpService.getsampleDatasources($scope.config.datasource.id).then(
                  function(response){
                    $scope.datasourceFields=$scope.iterate(response.data[0],"", []);
                  },
                  function(e){
                    console.log("Error getting datasourceFields: " +  JSON.stringify(e))
                  }
                );
              }
              else 
              {return null;}
        }


          $scope.save = function() { 
            $scope.config.type = $scope.type;
            $scope.config.content=parseProperties();         
            $scope.layergrid.push($scope.config);
            $mdDialog.cancel();
          };
        
        }
        $mdDialog.show({
          controller: AddGadgetController,
          templateUrl: 'app/partials/edit/addGadgetTemplateParameterDialog.html',
          parent: angular.element(document.body),
          clickOutsideToClose:true,
          fullscreen: false, // Only for -xs, -sm breakpoints.
          openFrom: '.sidenav-fab',
          closeTo: angular.element(document.querySelector('.sidenav-fab')),
          locals: {
            type: type,
            config: config,
            layergrid: layergrid
          }
        })
        .then(function() {

        }, function() {
          $scope.status = 'You cancelled the dialog.';
        });
      }






      function dropElementEvent(e,newElem){
        var type = e.dataTransfer.getData("type");
        newElem.id = type + "_" + (new Date()).getTime();
        newElem.content = type;
        newElem.type = type;
        //newElem.minItemRows = 10;
        //newElem.minItemCols = 10;
        //newElem.cols = 10;
        //newElem.rows = 10;
        newElem.header = {
          enable: true,
          title: {

           
            iconColor: "hsl( 206, 54%, 5%)",
            text: type + "_" + (new Date()).getTime(),
            textColor: "hsl(206,54%,5%)"
          },
          backgroundColor: "hsl(0, 0%, 100%)",
          height: 40
        }
        newElem.backgroundColor ="white";
        newElem.padding = 0;
        newElem.border = {

          color: "hsl(0, 0%, 90%)",
          width: 0,
          radius: 5
        }
        if(type == 'livehtml'){
          newElem.content = "<!-- Write your HTML <div></div> and CSS <style></style> here -->\n<!--Focus here and F11 to full screen editor-->";
          newElem.contentcode = "//Write your controller (JS code) code here\n\n//Focus here and F11 to full screen editor\n\n//This function will be call once to init components\nvm.initLiveComponent = function(){\n\n};\n\n//This function will be call when data change. On first execution oldData will be null\nvm.drawLiveComponent = function(newData, oldData){\n\n};\n\n//This function will be call on element resize\nvm.resizeEvent = function(){\n\n}\n\n//This function will be call when element is destroyed\nvm.destroyLiveComponent = function(){\n\n};\n\n//This function will be call on element resize\nvm.resizeEvent = function(){\n\n}\n\n//This function will be call when receiving a value from vm.sendValue(idGadgetTarget,data)\nvm.receiveValue = function(data){\n\n};";       
          showAddGadgetTemplateDialog(type,newElem,vm.dashboard.pages[vm.selectedpage].layers[vm.dashboard.pages[vm.selectedpage].selectedlayer].gridboard); 
        }else  if(type == 'gadgetfilter'){
          newElem.content = "<!-- Write your HTML <div></div> and CSS <style></style> here -->\n<!--Focus here and F11 to full screen editor-->";
          newElem.contentcode = "//Write your controller (JS code) code here\n\n//Focus here and F11 to full screen editor\n\n//This function will be call once to init components\nvm.initLiveComponent = function(){\n\n};\n\n//This function will be call when data change. On first execution oldData will be null\nvm.drawLiveComponent = function(newData, oldData){\n\n};\n\n//This function will be call on element resize\nvm.resizeEvent = function(){\n\n}\n\n//This function will be call when element is destroyed\nvm.destroyLiveComponent = function(){\n\n};\n\n//This function will be call on element resize\nvm.resizeEvent = function(){\n\n}\n\n//This function will be call when receiving a value from vm.sendValue(idGadgetTarget,data)\nvm.receiveValue = function(data){\n\n};";       
          addGadgetFilter(type,newElem,vm.dashboard.pages[vm.selectedpage].layers[vm.dashboard.pages[vm.selectedpage].selectedlayer].gridboard); 
        }
        else if(type == 'html5'){         
          addGadgetHtml5(type,newElem,vm.dashboard.pages[vm.selectedpage].layers[vm.dashboard.pages[vm.selectedpage].selectedlayer].gridboard); 
        }
        else{         
          showAddGadgetDialog(type,newElem,vm.dashboard.pages[vm.selectedpage].layers[vm.dashboard.pages[vm.selectedpage].selectedlayer].gridboard);
          
        }
      };


      function sendResizeToGadget(item, itemComponent) {
        $timeout(
          function(){
            $scope.$broadcast("$resize", "");
          },100
        );
      }
    };

    vm.checkIndex = function(index){
      return vm.selectedpage === index;
    }

    vm.setIndex = function(index){
      vm.selectedpage = index;
    }


    function showUrlParamDialog(parameters){
      function showUrlParamController($scope,__env, $mdDialog, httpService,  parameters) {
        $scope.parameters = parameters;
        $scope.hide = function() {
          $mdDialog.hide();
        };

        
        $scope.save = function() {
          var sPageURL = $window.location.pathname;	
          var url = urlParamService.generateUrlWithParam(sPageURL,$scope.parameters);
          $window.location.href = url;          	
        };
       
      }
      $mdDialog.show({
        controller: showUrlParamController,
        templateUrl: 'app/partials/edit/formUrlparamMandatoryDialog.html',
        parent: angular.element(document.body),
        clickOutsideToClose:false,
        fullscreen: true, // Only for -xs, -sm breakpoints.
        openFrom: '.sidenav-fab',
        closeTo: angular.element(document.querySelector('.sidenav-fab')),
        locals: {
          parameters: parameters
        }
      })
      .then(function() {
     
      }, function() {
        $scope.status = 'You cancelled the dialog.';
      });
    }



  }
})();
