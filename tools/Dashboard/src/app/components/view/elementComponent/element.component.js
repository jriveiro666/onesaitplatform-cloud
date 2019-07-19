(function () {
  'use strict';

  angular.module('dashboardFramework')
    .component('element', {
      templateUrl: 'app/components/view/elementComponent/element.html',
      controller: ElementController,
      controllerAs: 'vm',
      bindings:{
        element: "=",
        iframe: "=",
        editmode: "<"
      }
    });

  /** @ngInject */
  function ElementController($compile,$log, $scope, $mdDialog, $sce, $rootScope, $timeout, interactionService,filterService,$mdSidenav) {
    var vm = this;
    vm.isMaximized = false;
    vm.datastatus;
 
    //Contains the information of the filters
  
  //  vm.config=[{id:"filtro1",type:"numberfilter", field:"Helsinki.year",name:"year",op:">",typeAction:"filter",initialFilter:false,useLastValue:true,filterChaining:false,targetList:[{gadgetId:"livehtml_1550073936906",overwriteField:"Helsinki.year"},{gadgetId:"livehtml_1549895094697",overwriteField:"Helsinki.year"}],value:2000},
   //            {id:"filtro2",type:"textfilter", field:"Helsinki.population",name:"population",op:">",typeAction:"action",initialFilter:false,useLastValue:true,filterChaining:false,targetList:[{gadgetId:"livehtml_1550073936906",overwriteField:"Helsinki.year"}],value:""}];
    //vm.config=[{"type":"textfilter"}];

    vm.$onInit = function () {
      //Initialice filters      
      vm.config = vm.element.filters;
      if(typeof vm.element.hideBadges ==='undefined'){
        vm.element.hideBadges=true;
      }
      if(typeof vm.element.notshowDotsMenu ==='undefined'){
        vm.element.notshowDotsMenu=false;
      }
      inicializeIncomingsEvents(); 
      //Added config filters to interactionService hashmap      
      interactionService.registerGadgetFilters(vm.element.id,vm.config);      
    };

    vm.openMenu = function($mdMenu){
      $mdMenu.open();
    }

    

    vm.elemntbodyclass = function(){     
     var temp =''+vm.element.id+' '+vm.element.type;
      if(vm.element.header.enable === true ) {
        temp +=' '+'headerMargin';
        if(vm.element.hideBadges === true ) {
          temp +=' '+'withoutBadgesAndHeader';
         }else{
          temp +=' '+'withBadgesAndHeader';
         }
     }else{
        temp +=' '+'noheaderMargin';
        if(vm.element.hideBadges === true ) {
          temp +=' '+'withoutBadges';
         }else{
          temp +=' '+'withBadges';
         }
     }
    
   return temp;
    }

    function inicializeIncomingsEvents(){
      $scope.$on("global.style",
        function(ev,style){
          angular.merge(vm.element,vm.element,style);
        }
      );   
    }

    vm.openEditGadgetIframe = function(ev) {      
      $mdDialog.show({
        parent: angular.element(document.body),
        targetEvent: ev,
        fullscreen: false,
        template:
          '<md-dialog id="dialogCreateGadget"  aria-label="List dialog">' +
          '  <md-dialog-content >'+
          '<iframe id="iframeCreateGadget" style=" height: 80vh; width: 80vw;" frameborder="0" src="'+__env.endpointControlPanel+'/gadgets/updateiframe/'+vm.element.id+'"+></iframe>'+                     
          '  </md-dialog-content>' +             
          '</md-dialog>',
          locals: {
            element: vm.element
          },
        controller: DialogIframeEditGadgetController
     });
     function DialogIframeEditGadgetController($scope, $mdDialog, element) {
       $scope.element = element;
       $scope.closeDialog = function() {
         var gadgets =document.querySelectorAll( 'gadget' ) ;
         if(gadgets.length>0){
          for (var index = 0; index < gadgets.length; index++) {
            var gad = gadgets[index];
            angular.element(gad).scope().$$childHead.reloadContent();
          }        
        }
         $mdDialog.hide();
       }
      };


     };

     // toggle gadget to fullscreen and back.
     vm.toggleFullScreen = function(){               
      vm.isMaximized = !vm.isMaximized;
     
      //change overflow-y gridster 
      if(vm.isMaximized){
       $('gridster').css('overflow-y','hidden');
       $('gridster').css('overflow-x','hidden');
       $('gridster').animate({ scrollTop: 0 }, 1);
      }else{
       $('gridster').css('overflow-y','auto');
       $('gridster').css('overflow-x','auto');
       $('gridster').animate({ scrollTop: 0 }, 1);
      }
      $timeout(
         function(){
           $scope.$broadcast("$resize", "");
         },300
       );
    };


     vm.reloadFilters = function(){      
      angular.element( document.querySelector( '#_'+vm.element.id+'filters' ) ).empty();
      angular.element(document.getElementById('_'+vm.element.id+'filters')).append($compile('<filter id="vm.element.id" datasource="vm.element.datasource" config="vm.config" hidebuttonclear="vm.element.hidebuttonclear" buttonbig="false"></filter> ')($scope));      
      angular.element( document.querySelector( '#__'+vm.element.id+'filters' ) ).empty();
      angular.element(document.getElementById('__'+vm.element.id+'filters')).append($compile('<filter id="vm.element.id" datasource="vm.element.datasource" config="vm.config" hidebuttonclear="vm.element.hidebuttonclear" buttonbig="false"></filter> ')($scope));      
     }

     vm.showfiltersInModal = function (){
      //if iframe show on menu
      if(vm.element.type==='gadgetfilter'){
        return false
      }
      if( vm.element.filtersInModal === true 
            && vm.config!=null 
            && showFilters(vm.config)){             
        return true;
      }      
      return false;
     }

     vm.showFiltersInBody = function (){
        //hide when is a gadget filter 
      if(vm.element.type==='gadgetfilter'){
         return false
       }
        //if iframe show on menu
        if((typeof vm.element.filtersInModal === 'undefined' || vm.element.filtersInModal === false) 
              && vm.config!=null 
              && showFilters(vm.config)){         
          return true;
        }     
        return false;
     }


     function showFilters(config){
      if(config.length>0){
        for (var index = 0; index < config.length; index++) {
          var element = config[index];
            if(typeof element.hide === 'undefined' || element.hide === false){
              return true;
            }          
        }
      }
        return false;      
     }


    vm.openEditContainerDialog = function (ev) {
      $mdDialog.show({
        controller: EditContainerDialog,
        templateUrl: 'app/partials/edit/editContainerDialog.html',
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose:false,
        multiple : true,
        fullscreen: false, // Only for -xs, -sm breakpoints.
        locals: {
          element: vm.element
        }
      })
      .then(function(answer) {
      }, function() {
        $scope.status = 'You cancelled the dialog.';
      });
    };

    function EditContainerDialog($scope, $mdDialog,utilsService, element) {
      $scope.icons = utilsService.icons;

      $scope.element = element;

      $scope.queryIcon = function (query) {
        return query ? $scope.icons.filter( createFilterFor(query) ) : $scope.icons;
      }

      /**
       * Create filter function for a query string
       */
      function createFilterFor(query) {
        var lowercaseQuery = angular.lowercase(query);
        return function filterFn(icon) {
          return (icon.indexOf(lowercaseQuery) != -1);
        };
      }

      $scope.hide = function() {
        $mdDialog.hide();
      };

      $scope.cancel = function() {
        $mdDialog.cancel();
      };

      $scope.answer = function(answer) {
        $mdDialog.hide(answer);
      };
    }

    function EditGadgetDialog($scope, $timeout,$mdDialog,  element, contenteditor, httpService) {
      
      $scope.codemirrorLoaded = function(_editor){
        // Editor part
        var _doc = _editor.getDoc();
        _editor.focus();
        $scope.refreshCodemirror = true;
        $timeout(function () {
          $scope.refreshCodemirror = false;
        }, 100);
        $scope.loadDatasources();
      };

      var codemirrorBaseConfig = {
        lineWrapping: false, 
        fixedGutter: false, 
        lineNumbers: true, 
        theme:'twilight', 
        autofocus: true, 
        autoRefresh:true, 
        onLoad : $scope.codemirrorLoaded, 
        extraKeys: {
          'F11': function(cm) {
            cm.setOption('fullScreen', !cm.getOption('fullScreen'));
            //Fix full screen outside md-dialog
            if(cm.getOption('fullScreen')){
              document.getElementsByTagName("md-dialog")[0].style.maxWidth = "100%";
              document.getElementsByTagName("md-dialog")[0].style.maxHeight = "100%";
              document.getElementsByTagName("md-dialog")[0].style.left = "0";
              document.getElementsByTagName("md-dialog")[0].style.right = "0";
              document.getElementsByTagName("md-dialog")[0].style.top = "0";
              document.getElementsByTagName("md-dialog")[0].style.bottom = "0";
              document.getElementsByTagName("md-dialog")[0].style.position = "fixed";
            }
            else{
              document.getElementsByTagName("md-dialog")[0].style.maxWidth = "";
              document.getElementsByTagName("md-dialog")[0].style.maxHeight = "";
              document.getElementsByTagName("md-dialog")[0].style.left = "";
              document.getElementsByTagName("md-dialog")[0].style.right = "";
              document.getElementsByTagName("md-dialog")[0].style.top = "";
              document.getElementsByTagName("md-dialog")[0].style.bottom = "";
              document.getElementsByTagName("md-dialog")[0].style.position = "";
            }
          },
          'Esc': function(cm) {
            if (cm.getOption('fullScreen')) 
              cm.setOption('fullScreen', false);
              document.getElementsByTagName("md-dialog")[0].style.maxWidth = "";
              document.getElementsByTagName("md-dialog")[0].style.maxHeight = "";
              document.getElementsByTagName("md-dialog")[0].style.left = "";
              document.getElementsByTagName("md-dialog")[0].style.right = "";
              document.getElementsByTagName("md-dialog")[0].style.top = "";
              document.getElementsByTagName("md-dialog")[0].style.bottom = "";
              document.getElementsByTagName("md-dialog")[0].style.position = "";
          }
        }
      }
      
      $scope.htmlcodeoptions = angular.copy(codemirrorBaseConfig);
      $scope.htmlcodeoptions.mode = 'htmlmixed';

      $scope.jscodeoptions = angular.copy(codemirrorBaseConfig);
      $scope.jscodeoptions.mode = 'javascript';

      $scope.editor;
      
      $scope.contenteditor = contenteditor;
      
      $scope.livecompilation = false;

      $scope.element = element;

      function refreshRealCode(){
        if($scope.livecompilation){
          if(typeof $scope.contenteditor.html !== 'undefined'){
            $scope.element.content = $scope.contenteditor.html.slice();
          }else{
            $scope.element.content = "";
          }
          if(typeof $scope.contenteditor.js !== 'undefined'){
            $scope.element.contentcode = $scope.contenteditor.js.slice();
          }else{
            $scope.element.contentcode = "";
          }
        
        }
      }

      $scope.$watchGroup(['contenteditor.html','contenteditor.js'], refreshRealCode, true);

      $scope.compile = function(){       

        if(typeof $scope.contenteditor.html !== 'undefined'){
          $scope.element.content = $scope.contenteditor.html.slice();
        }else{
          $scope.element.content = "";
        }
        if(typeof $scope.contenteditor.js !== 'undefined'){
          $scope.element.contentcode = $scope.contenteditor.js.slice();
        }else{
          $scope.element.contentcode = "";
        }

      }

      $scope.hide = function() {
        $mdDialog.hide();
      };

      $scope.cancel = function() {
        $mdDialog.cancel();
      };

      $scope.answer = function(answer) {
        $mdDialog.hide(answer);
      };

      $scope.datasources = [];

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

    }

    vm.openEditGadgetDialog = function (ev) {
      if(!vm.contenteditor){
        vm.contenteditor = {}
        vm.contenteditor["html"] = vm.element.content.slice();
        vm.contenteditor["js"] = vm.element.contentcode.slice();
      }
      $mdDialog.show({
        controller: EditGadgetDialog,
        templateUrl: 'app/partials/edit/editGadgetDialog.html',
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose:true,
        multiple : true,
        fullscreen: false, // Only for -xs, -sm breakpoints.
       
        locals: {
          element: vm.element,
          contenteditor: vm.contenteditor
        }
      })
      .then(function(answer) {
       
      }, function() {
        $scope.status = 'You cancelled the dialog.';
      });
    
    };

    function EditGadgetHTML5Dialog($timeout,$scope, $mdDialog,  element, httpService) {
      $scope.editor;
      
      $scope.element = element;

      $scope.codemirrorLoaded = function(_editor){
        // Editor part
        var _doc = _editor.getDoc();
        _editor.focus();
        $scope.refreshCodemirror = true;
        $timeout(function () {
          $scope.refreshCodemirror = false;
        }, 100);
      };

      var codemirrorBaseConfig = {
        lineWrapping: false, 
        fixedGutter: false, 
        lineNumbers: true, 
        theme:'twilight', 
        autofocus: true, 
        autoRefresh:true, 
        onLoad : $scope.codemirrorLoaded, 
        extraKeys: {
          'F11': function(cm) {
            cm.setOption('fullScreen', !cm.getOption('fullScreen'));
            //Fix full screen outside md-dialog
            if(cm.getOption('fullScreen')){
              document.getElementsByTagName("md-dialog")[0].style.maxWidth = "100%";
              document.getElementsByTagName("md-dialog")[0].style.maxHeight = "100%";
              document.getElementsByTagName("md-dialog")[0].style.left = "0";
              document.getElementsByTagName("md-dialog")[0].style.right = "0";
              document.getElementsByTagName("md-dialog")[0].style.top = "0";
              document.getElementsByTagName("md-dialog")[0].style.bottom = "0";
              document.getElementsByTagName("md-dialog")[0].style.position = "fixed";
            }
            else{
              document.getElementsByTagName("md-dialog")[0].style.maxWidth = "";
              document.getElementsByTagName("md-dialog")[0].style.maxHeight = "";
              document.getElementsByTagName("md-dialog")[0].style.left = "";
              document.getElementsByTagName("md-dialog")[0].style.right = "";
              document.getElementsByTagName("md-dialog")[0].style.top = "";
              document.getElementsByTagName("md-dialog")[0].style.bottom = "";
              document.getElementsByTagName("md-dialog")[0].style.position = "";
            }
          },
          'Esc': function(cm) {
            if (cm.getOption('fullScreen')) 
              cm.setOption('fullScreen', false);
              document.getElementsByTagName("md-dialog")[0].style.maxWidth = "";
              document.getElementsByTagName("md-dialog")[0].style.maxHeight = "";
              document.getElementsByTagName("md-dialog")[0].style.left = "";
              document.getElementsByTagName("md-dialog")[0].style.right = "";
              document.getElementsByTagName("md-dialog")[0].style.top = "";
              document.getElementsByTagName("md-dialog")[0].style.bottom = "";
              document.getElementsByTagName("md-dialog")[0].style.position = "";
          }
        }
      }

      $scope.htmlcodeoptions = angular.copy(codemirrorBaseConfig);
      $scope.htmlcodeoptions.mode = 'htmlmixed';    

      $scope.contenteditor = element.content.slice();

      $scope.livecompilation = false;

      function refreshRealCode(){
        if($scope.livecompilation){
          $scope.element.content = $scope.contenteditor.slice();
        }
      }

      $scope.$watchGroup(['contenteditor'], refreshRealCode, true);

      $scope.compile = function(){
        $scope.element.content = $scope.contenteditor.slice();
      }

      $scope.hide = function() {
        $mdDialog.hide();
      };

      $scope.cancel = function() {
        $mdDialog.cancel();
      };

      $scope.answer = function(answer) {
        $mdDialog.hide(answer);
      };

    }


    vm.openEditGadgetHTML5Dialog = function (ev) {
      $mdDialog.show({
        controller: EditGadgetHTML5Dialog,
        templateUrl: 'app/partials/edit/editGadgetHTML5Dialog.html',
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose:true,
        multiple : true,
        fullscreen: false, // Only for -xs, -sm breakpoints.
       
        locals: {
          element: vm.element
        }
      })
      .then(function(answer) {
       
      }, function() {
        $scope.status = 'You cancelled the dialog.';
      });
    };

    vm.trustHTML = function(html_code) {
      return $sce.trustAsHtml(html_code)
    }

    vm.calcHeight = function(){
      vm.element.header.height = (vm.element.header.height=='inherit'?25:vm.element.header.height);     
     var result = "'calc(100% - 36px)'";


      return result;
    }


    vm.toggleRight =  function(componentId) {
      $mdSidenav(componentId).toggle();
    };
    
    
    
    
    
    vm.deleteElement = function(){
      $rootScope.$broadcast("deleteElement",vm.element);
    }

    vm.generateFilterInfo = function(filter){ 
      return filter.value;
    }

    vm.deleteFilter = function(id, field,op){      
      $rootScope.$broadcast(vm.element.id,{id: id,type:'filter',data:[],field:field,op:op})
    }




    vm.openFilterDialog = function(ev) {     
      $mdDialog.show({
        parent: angular.element(document.body),
        targetEvent: ev,
        scope: $scope,
        preserveScope: true, 
        fullscreen: false,
        template:
          '<md-dialog flex="35"  aria-label="List dialog" style="min-width:440px">' +
          '<form ng-cloak>'+
          '<md-toolbar style="background-color:rgba(255,255,255,0.87);  position: absolute; top: 0;right: 0;">' +
          '<div class="md-toolbar-tools">' +
          '<span flex="" class="flex"></span>'+
          '<button type="button" aria-label="Close" class="ods-dialog__headerbtn" ng-click="closeDialog()"><span class="ods-dialog__close ods-icon ods-icon-close"></span></button>'+           
          '</div>' +
       ' </md-toolbar>' +
          '  <md-dialog-content style="padding: 30px 30px 10px;" >'+
          ' <filter id="vm.element.id" datasource="vm.element.datasource" config="vm.config" hidebuttonclear="vm.element.hidebuttonclear" buttonbig="true"></filter>'+  
          '  </md-dialog-content>' + 
          '</form>'+           
          '</md-dialog>',
         
        controller: function DialogController($scope, $mdDialog) {
      
          $scope.closeDialog = function() {
            $mdDialog.hide();
          }
        }
     });
    

     };


     vm.openEditFilterDialog = function (ev) {
      $mdDialog.show({
        controller: EditFilterDialog,
        templateUrl: 'app/partials/edit/editFilterDialog.html',
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose:false,
        multiple : true,
        fullscreen: false, // Only for -xs, -sm breakpoints.
        locals: {
          element: vm.element
        }
      })
      .then(function(answer) {        
        interactionService.registerGadgetFilters(vm.element.id,vm.element.filters);        
        vm.config = vm.element.filters;
        vm.reloadFilters();
      }, function() {
        $scope.status = 'You cancelled the dialog.';             
        interactionService.registerGadgetFilters(vm.element.id,vm.element.filters);        
        vm.config = vm.element.filters;
        vm.reloadFilters();
      
      });
    };

    function EditFilterDialog($scope, $mdDialog,utilsService,httpService, element,gadgetManagerService) {
     

      $scope.element = element;
     
      $scope.tempFilter = {data:{},typeAction: "filter"};
      $scope.typeList = [
                          {id:'textfilter',description:'text filter'},
                          {id:'numberfilter',description:'numerical filter'},
                          {id:'livefilter',description:'Date range and real time filter'},
                          {id:'intervaldatefilter',description:'Date range filter'},                          
                          {id:'simpleselectfilter',description:'text filter with simple-selection'},
                          {id:'simpleselectnumberfilter',description:'numerical filter with simple-selection'},
                          {id:'multiselectfilter',description:'text filter with multi-selection'},
                          {id:'multiselectnumberfilter',description:'numerical filter with multi-selection'}
                        ];
      
                        
      $scope.opList = [
        {id:'=',description:'='},
        {id:'>',description:'>'},
        {id:'<',description:'<'},
        {id:'<=',description:'<='},
        {id:'>=',description:'>='},
        {id:'<>',description:'<>'}
      ];
     
      
     
      $scope.hideLabelName = true;
      $scope.hideOperator = true;
      $scope.hideValue = true;
      $scope.hideOptions = true; 

     generateGadgetsLists();

     function generateGadgetsLists(){
     
      $scope.gadgetsTargets = getGadgetsInDashboard();
      refreshGadgetTargetFields($scope.element.id);
    }

    //Generate gadget list of posible Sources of interactions: pie, bar, livehtml
    function getGadgetsInDashboard(){
      return gadgetManagerService.returnGadgets();
    }


    function findGadgetInDashboard(gadgetId){    
        return gadgetManagerService.findGadgetById(gadgetId)
    }

    $scope.generateGadgetInfo = function (gadgetId){
      var gadget = findGadgetInDashboard(gadgetId);
      if(gadget == null){
        return gadgetId;
      }
      else{
        return $scope.prettyGadgetInfo(gadget);
      }
    }


    

    $scope.deleteOption = function (opt){
      var options = $scope.tempFilter.data.options;
       
          for(var i=0;i<options.length;i++){
            if(options[i] === opt ){
              options.splice(i, 1); 
              break;
            }
          }
    }

    $scope.addOption = function (opt){
     
      if(typeof $scope.tempFilter.data ==='undefined' || typeof $scope.tempFilter.data.options ==='undefined'){
        $scope.tempFilter.data = {'options':[opt]};
      }else{
        var find = false;
        for(var i=0;i<$scope.tempFilter.data.options.length;i++){
          if($scope.tempFilter.data.options[i] === opt ){            
            return null;;
          }
        }
        $scope.tempFilter.data.options.push(opt); 
      }
       
    }

    

    $scope.deleteFilter = function (id){
      if(typeof $scope.element.filters !=='undefined' ){
        for (var index = 0; index < $scope.element.filters.length; index++) {         
          if($scope.element.filters[index].id === id){           
            $scope.element.filters.splice(index, 1);            
            return null;
          }          
        }
      }
    }

    

    $scope.editFilter = function (id){
      if(typeof $scope.element.filters !=='undefined' ){
        for (var index = 0; index < $scope.element.filters.length; index++) {         
          if($scope.element.filters[index].id === id){
            
            $scope.tempFilter = makeFilter( $scope.element.filters[index],true);
            //update
            $scope.hideFields($scope.tempFilter.type);        
            return null;
          }          
        }
      }
    }
    





    $scope.addFilter = function(){
      //validations
      var tempFilter = $scope.tempFilter;
     
      tempFilter.typeAction = "filter";
      var targetGadgetField = $scope.targetGadgetField;

      if(typeof tempFilter.id ==='undefined' || (typeof tempFilter.id !=='undefined' && tempFilter.id.length===0)){
        //identifier mandatory
        return null;
      }
     
      if(typeof tempFilter.type ==='undefined' || (typeof tempFilter.type !=='undefined' && tempFilter.type.length===0)){
        //type mandatory
        return null;
      }
      if(typeof tempFilter.name ==='undefined' || (typeof tempFilter.name !=='undefined' && tempFilter.name.length===0)){
      
        tempFilter.name="";

      }
      if(tempFilter.typeAction==='filter'&&( tempFilter.type ==='textfilter' || tempFilter.type==='numberfilter' )){
        if( typeof tempFilter.op ==='undefined' || (typeof tempFilter.op !=='undefined' && tempFilter.op.length===0)){
          //   op mandatory 
          return null;
        }
      }    

      if(typeof targetGadgetField ==='undefined' || targetGadgetField===null || targetGadgetField.trim().length === 0){
        //targetList mandatory
        return null;
      }

      tempFilter.targetList=[{
        "gadgetId": $scope.element.id,
        "overwriteField": targetGadgetField
      }];

        //update for id 
      if(typeof $scope.element.filters !=='undefined' ){
        for (var index = 0; index < $scope.element.filters.length; index++) {
          var elem = $scope.element.filters[index];
          if(elem.id === tempFilter.id){           
            $scope.element.filters[index] = makeFilter(tempFilter,false) ;
            return null;
          }          
        }
      }
      if(typeof  $scope.element.filters === 'undefined'){
        $scope.element.filters = [makeFilter(tempFilter,false)];
      }else{
        $scope.element.filters.push( makeFilter(tempFilter,false) );
      }
    }


function makeFilter(tempFilter,read){
  //load for edit
  if(read){
    var filter = {
      'id':tempFilter.id,
      'type': tempFilter.type,
      'typeAction': tempFilter.typeAction,
      'name':tempFilter.name,
      'op': tempFilter.op,
      'value': tempFilter.value,
      'targetList':tempFilter.targetList,
      'hide':tempFilter.hide,
      'initialFilter':tempFilter.initialFilter,
      'data':tempFilter.data
    };
    $scope.targetGadgetField=tempFilter.targetList[0].overwriteField;
  }
  else{
    //for create new data or update
    for (var index = 0; index < tempFilter.targetList.length; index++) {    
      tempFilter.targetList[index].field = tempFilter.targetList[index].overwriteField;
    }
    if(tempFilter.type === 'multiselectfilter' || tempFilter.type === 'multiselectnumberfilter'  ){
      if(typeof tempFilter.data!='undefined' && typeof tempFilter.data.options!='undefined' ){
        tempFilter.data.optionsSelected = tempFilter.data.options.slice();
      }
    }
    if(tempFilter.type === 'simpleselectfilter' || tempFilter.type === 'simpleselectnumberfilter'  ){
      if(typeof tempFilter.data!='undefined' && typeof tempFilter.data.options!='undefined' ){
        tempFilter.data.optionsSelected = tempFilter.value;
      }
    }
    if(tempFilter.type === 'livefilter'){
      tempFilter.data = {
        "options": null,
        "optionsSelected": null,
        "startDate": "NOW(\"yyyy-MM-dd'T'HH:mm:ss'Z'\",\"hour\",-8)",
        "endDate": "NOW(\"yyyy-MM-dd'T'HH:mm:ss'Z'\",\"hour\",0)",
        "selectedPeriod": 8,
        "realtime": "start"
      };
    }
    if(tempFilter.type === 'intervaldatefilter'){
      tempFilter.data = {
        "options": null,
        "optionsSelected": null,
        "startDate":  moment().subtract(8,'hour').toISOString() ,
        "endDate":  moment().toISOString() 
      };
    }

    var filter = {
      'id':tempFilter.id,
      'typeAction': tempFilter.typeAction,
      'type': tempFilter.type,
      'name':tempFilter.name,
      'op': tempFilter.op,
      'value': tempFilter.value,
      'targetList':tempFilter.targetList,
      'hide':tempFilter.hide,
      'initialFilter':tempFilter.initialFilter,
      'data':tempFilter.data
    };
  }
return filter;
}

    function refreshGadgetTargetFields (gadgetId){
      var gadget = findGadgetInDashboard(gadgetId);
      if(gadget == null){
        $scope.gadgetEmitterFields = [];
      }
      else{
        setGadgetTargetFields(gadget);
      }
    }


 //Destination are all gadget fields
 function setGadgetTargetFields(gadget){        
    $scope.targetDatasource="";
  var gadgetData = angular.element(document.getElementsByClassName(gadget.id)[0]).scope().$$childHead.vm;
  if(gadget.type === 'livehtml'){
    if(typeof gadgetData.datasource!=='undefined'){
      $scope.targetDatasource = gadgetData.datasource.name;
     
    }else{
      $scope.gadgetTargetFields = [];
      return null;
    }
    var dsId = gadgetData.datasource.id;
  } else  if(gadget.type === 'gadgetfilter'){
    if(typeof gadgetData.datasource!=='undefined'){
      $scope.targetDatasource = gadgetData.datasource.name;     
    }else{
      $scope.gadgetTargetFields = [];
      return null;
    }
    var dsId = gadgetData.datasource.id;
  }
  else{
    $scope.targetDatasource = gadgetData.measures[0].datasource.identification;
    var dsId = gadgetData.measures[0].datasource.id;
  }
  httpService.getFieldsFromDatasourceId(dsId).then(
    function(data){
      $scope.gadgetTargetFields = utilsService.transformJsonFieldsArrays(utilsService.getJsonFields(data.data[0],"", []));
    }
  )
  $scope.gadgetTargetFields = [];
}

 //Get gadget JSON and return string info for UI
 $scope.prettyGadgetInfo = function(gadget){
       
  return gadget.header.title.text + " (" + gadget.type + ")";

}


$scope.queryTargetField = function(query){     
  $scope.targetDatasource="";
var gadgetData = angular.element(document.getElementsByClassName($scope.targetGadget)[0]).scope().$$childHead.vm;
if(gadgetData.type === 'livehtml'){
  if(typeof gadgetData.datasource!=='undefined'){
    $scope.targetDatasource = gadgetData.datasource.name;
   
  }else{
    return [];
   
  }
  var dsId = gadgetData.datasource.id;
} else if(gadgetData.type === 'gadgetfilter'){
  if(typeof gadgetData.datasource!=='undefined'){
    $scope.targetDatasource = gadgetData.datasource.name;
   
  }else{
    return [];
   
  }
  var dsId = gadgetData.datasource.id;
}
else{
  $scope.targetDatasource = gadgetData.measures[0].datasource.identification;
  var dsId = gadgetData.measures[0].datasource.id;
}
httpService.getFieldsFromDatasourceId(dsId).then(
  function(data){
   var gadgetTargetFields = utilsService.transformJsonFieldsArrays(utilsService.getJsonFields(data.data[0],"", []));
   var result = query ? gadgetTargetFields.filter(createFilterFor(query)) : gadgetTargetFields;
   return result;
  }
)
return  [];
}

    
      /**
       * Create filter function for a query string
       */
      function createFilterFor(query) {
        var lowercaseQuery = query.toLowerCase();  
        return function filterFn(field) {
          return (field.field.toLowerCase().indexOf(lowercaseQuery) === 0); 
        };
  
      }



$scope.hideFields = function(type){
  if($scope.tempFilter.typeAction==='filter'){

    if(type==='textfilter'){
      $scope.hideLabelName = false;
      $scope.hideOperator = false;
      $scope.hideValue = false;
      $scope.hideOptions = true; 
      $scope.hideInitialFilter = false;
      $scope.hideHide =false;
    }else if(type==='numberfilter'){
      $scope.hideLabelName = false;
      $scope.hideOperator = false;
      $scope.hideValue = false;
      $scope.hideOptions = true; 
      $scope.hideInitialFilter = false;
      $scope.hideHide =false;
    }else if(type==='livefilter'){
      $scope.hideLabelName = true;
      $scope.hideOperator = true;
      $scope.hideValue = true;
      $scope.hideOptions = true; 
      $scope.hideInitialFilter = false;
      $scope.hideHide =false;
    }else if(type==='multiselectfilter'){
      $scope.hideLabelName = false;
      $scope.hideOperator = true;
      $scope.hideValue = true;
      $scope.hideOptions = false; 
      $scope.hideInitialFilter = false;
      $scope.hideHide =false;
    }else if(type==='multiselectnumberfilter'){
      $scope.hideLabelName = false;
      $scope.hideOperator = true;
      $scope.hideValue = true;
      $scope.hideOptions = false; 
      $scope.hideInitialFilter = false;
      $scope.hideHide =false;
    }else if(type==='simpleselectfilter'){
      $scope.hideLabelName = false;
      $scope.hideOperator = false;
      $scope.hideValue = true;
      $scope.hideOptions = false; 
      $scope.hideInitialFilter = true;
      $scope.hideHide =true;
    }else if(type==='simpleselectnumberfilter'){
      $scope.hideLabelName = false;
      $scope.hideOperator = false;
      $scope.hideValue = true;
      $scope.hideOptions = false; 
      $scope.hideInitialFilter = true;
      $scope.hideHide =true;
    }
    else if(type==='intervaldatefilter'){
      $scope.hideLabelName = false;
      $scope.hideOperator = true;
      $scope.hideValue = true;
      $scope.hideOptions = true; 
      $scope.hideInitialFilter = false;
      $scope.hideHide =false;
    }
  }else{
    $scope.hideLabelName = false;
    $scope.hideOperator = true;
    $scope.hideValue = false;
    $scope.hideOptions = true; 
    $scope.hideInitialFilter = true;
    $scope.hideHide =false;
  }

}


      $scope.hide = function() {
        $mdDialog.hide();
      };

      $scope.cancel = function() {
        $mdDialog.cancel();
      };

      $scope.answer = function(answer) {
        $mdDialog.hide(answer);
      };
    }


  }
})();
