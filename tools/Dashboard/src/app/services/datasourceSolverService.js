(function () {
  'use strict';

  angular.module('dashboardFramework')
    .service('datasourceSolverService', DatasourceSolverService);

  /** @ngInject */
  function DatasourceSolverService(socketService, httpService, $mdDialog, $interval, $rootScope, urlParamService) {
      var vm = this;
      vm.gadgetToDatasource = {};
      
      vm.pendingDatasources = {};
      vm.poolingDatasources = {};
      vm.streamingDatasources = {};

      //Adding dashboard for security comprobations
      vm.dashboard = $rootScope.dashboard?$rootScope.dashboard:"";

      httpService.setDashboardEngineCredentialsAndLogin().then(function(a){
        console.log("Login Rest OK, connecting SockJs Stomp dashboard engine");
        socketService.connect();
      }).catch(function(e){
        console.log("Dashboard Engine Login Rest Fail: " + JSON.stringify(e));
        $mdDialog.show(
          $mdDialog.alert()
            .parent(angular.element(document.querySelector('body')))
            .clickOutsideToClose(true)
            .title('Dashboard Engine Connection Fail')
            .textContent('Dashboard engine could not to be running, please start it and reload this page')
            .ariaLabel('Alert Dialog Dashboard Engine')
            .ok('OK')
           
        );
      })

      //datasource {"name":"name","type":"query","refresh":"refresh",triggers:[{params:{where:[],project:[],filter:[]},emiter:""}]}
     

      function connectRegisterSingleDatasourceAndFirstShot(datasource){
        
        if(datasource.type=="query"){//Query datasource. We don't need RT conection only request-response
          if(datasource.refresh==0){//One shot datasource, we don't need to save it, only execute it once
            
            for(var i = 0; i< datasource.triggers.length;i++){
              socketService.connectAndSendAndSubscribe([{"msg":fromTriggerToMessage(datasource.triggers[i],datasource.name),id: datasource.triggers[i].emitTo, callback: vm.emitToTargets}]);
            }
          }
          else{//Interval query datasource, we need to register this datasource in order to pooling results
            vm.poolingDatasources[datasource.name] = datasource;
            var intervalId = $interval(/*Datasource passed as parameter in order to call every refresh time*/
              function(datasource){
                for(var i = 0; i< datasource.triggers.length;i++){
                  if(typeof datasource.triggers[i].isActivated === "undefined" || datasource.triggers[i].isActivated){
                    socketService.connectAndSendAndSubscribe([{"msg":fromTriggerToMessage(datasource.triggers[i],datasource.name),id: datasource.triggers[i].emitTo, callback: vm.emitToTargets}]);
                  }                  
                }
              },datasource.refresh * 1000, 0, true, datasource
            );
            vm.poolingDatasources[datasource.name].intervalId = intervalId;
          }
        }
        else{//Streaming datasource
         
        }
      }

      //Method from gadget to drill up and down the datasource
      vm.drillDown = function(gadgetId){}
      vm.drillUp = function(gadgetId){}

      vm.updateDatasourceTriggerAndShot = function(gadgetID, updateInfo){        
        var accessInfo = vm.gadgetToDatasource[gadgetID];
        if(typeof accessInfo !== 'undefined'){
          var dsSolver = vm.poolingDatasources[accessInfo.ds].triggers[accessInfo.index];
          if(updateInfo!=null && updateInfo.constructor === Array){
            for(var index in updateInfo){
              updateQueryParams(dsSolver,updateInfo[index]);
            }
          }else{
            updateQueryParams(dsSolver,updateInfo);
          }        
          var solverCopy = angular.copy(dsSolver);       
          solverCopy.params.filter = urlParamService.generateFiltersForGadgetId(gadgetID);
          for(var index in dsSolver.params.filter){
            var bundleFilters = dsSolver.params.filter[index].data;
            for(var indexB in bundleFilters){
              solverCopy.params.filter.push(bundleFilters[indexB]);
            }
          }
          socketService.sendAndSubscribe({"msg":fromTriggerToMessage(solverCopy,accessInfo.ds),id: angular.copy(gadgetID), type:"filter", callback: vm.emitToTargets});
        }
      }

      vm.updateDatasourceTriggerAndRefresh = function(gadgetID, updateInfo){        
        var accessInfo = vm.gadgetToDatasource[gadgetID];
        var dsSolver = vm.poolingDatasources[accessInfo.ds].triggers[accessInfo.index];
        if(updateInfo!=null && updateInfo.constructor === Array){
          for(var index in updateInfo){
            updateQueryParams(dsSolver,updateInfo[index]);
          }
        }else{
          updateQueryParams(dsSolver,updateInfo);
        }        
        var solverCopy = angular.copy(dsSolver);       
        solverCopy.params.filter = urlParamService.generateFiltersForGadgetId(gadgetID);
        for(var index in dsSolver.params.filter){
          var bundleFilters = dsSolver.params.filter[index].data;
          for(var indexB in bundleFilters){
            solverCopy.params.filter.push(bundleFilters[indexB]);
          }
        }
        socketService.sendAndSubscribe({"msg":fromTriggerToMessage(solverCopy,accessInfo.ds),id: angular.copy(gadgetID), type:"refresh", callback: vm.emitToTargets});
      }

      vm.startRefreshIntervalData = function(gadgetID){        
        var accessInfo = vm.gadgetToDatasource[gadgetID];
        var dsSolver = vm.poolingDatasources[accessInfo.ds].triggers[accessInfo.index];
        dsSolver.isActivated = true;
        var solverCopy = angular.copy(dsSolver);       
        solverCopy.params.filter = urlParamService.generateFiltersForGadgetId(gadgetID);
        for(var index in dsSolver.params.filter){
          var bundleFilters = dsSolver.params.filter[index].data;
          for(var indexB in bundleFilters){
            solverCopy.params.filter.push(bundleFilters[indexB]);
          }
        }
        socketService.sendAndSubscribe({"msg":fromTriggerToMessage(solverCopy,accessInfo.ds),id: angular.copy(gadgetID), type:"refresh", callback: vm.emitToTargets});
      }

      vm.stopRefreshIntervalData = function(gadgetID){        
        var accessInfo = vm.gadgetToDatasource[gadgetID];
        var dsSolver = vm.poolingDatasources[accessInfo.ds].triggers[accessInfo.index];
        dsSolver.isActivated = false;
      }

      vm.refreshIntervalData = function(gadgetID){        
        var accessInfo = vm.gadgetToDatasource[gadgetID];
        var dsSolver = vm.poolingDatasources[accessInfo.ds].triggers[accessInfo.index];       
        var solverCopy = angular.copy(dsSolver);       
        solverCopy.params.filter = urlParamService.generateFiltersForGadgetId(gadgetID);
        for(var index in dsSolver.params.filter){
          var bundleFilters = dsSolver.params.filter[index].data;
          for(var indexB in bundleFilters){
            solverCopy.params.filter.push(bundleFilters[indexB]);
          }
        }
        socketService.sendAndSubscribe({"msg":fromTriggerToMessage(solverCopy,accessInfo.ds),id: angular.copy(gadgetID), type:"refresh", callback: vm.emitToTargets});
      }


      //update info has the filter, group, project id to allow override filters from same gadget and combining with others
      function updateQueryParams(trigger, updateInfo){
        var index = 0;//index filter
        var overwriteFilter = trigger.params.filter.filter(function(sfilter,i){
          if(sfilter.id == updateInfo.filter.id){
            index = i;
          }
          return sfilter.id == updateInfo.filter.id;
        });
        if (overwriteFilter.length>0){//filter founded, we need to override it
          if(updateInfo.filter.data.length==0){//with empty array we delete it, remove filter action
            trigger.params.filter.splice(index,1); 
          }
          else{ //override filter, for example change filter data and no adding
            overwriteFilter[0].data = updateInfo.filter.data;  
          }
        }
        else{
          trigger.params.filter.push(updateInfo.filter);
        }

        if(updateInfo.group){//For group that only change in drill options, we need to override all elements
          trigger.params.group = updateInfo.group;
        }

        if(updateInfo.project){//For project that only change in drill options, we need to override all elements
          trigger.params.project = updateInfo.project;
        }
      }

      vm.registerSingleDatasourceAndFirstShot = function(datasource, firstShot){
        if(datasource.type=="query"){//Query datasource. We don't need RT conection only request-response
          if(!(datasource.name in vm.poolingDatasources)){
            vm.poolingDatasources[datasource.name] = datasource;
            vm.poolingDatasources[datasource.name].triggers[0].listeners = 1;
            vm.gadgetToDatasource[datasource.triggers[0].emitTo] = {"ds":datasource.name, "index":0};
          }
          else if(!(datasource.triggers[0].emitTo in vm.gadgetToDatasource)){
            vm.poolingDatasources[datasource.name].triggers.push(datasource.triggers[0]);
            var newposition = vm.poolingDatasources[datasource.name].triggers.length-1
            vm.poolingDatasources[datasource.name].triggers[newposition].listeners = 1;
            vm.gadgetToDatasource[datasource.triggers[0].emitTo] = {"ds":datasource.name, "index":newposition};
          }
          else{
            var gpos = vm.gadgetToDatasource[datasource.triggers[0].emitTo];
            vm.poolingDatasources[datasource.name].triggers[gpos.index].listeners++;
          }
          //One shot datasource, for pooling and          
          if(firstShot!=null && firstShot){
            for(var i = 0; i< datasource.triggers.length;i++){
              console.log("firstShot",datasource.triggers);
             socketService.sendAndSubscribe({"msg":fromTriggerToMessage(datasource.triggers[i],datasource.name),id: angular.copy(datasource.triggers[i].emitTo), type:"refresh", callback: vm.emitToTargets});
            }
          }
          if(datasource.refresh!=0){//Interval query datasource, we need to register this datasource in order to pooling results
            var i;
            var intervalId = $interval(/*Datasource passed as parameter in order to call every refresh time*/
              function(datasource){                
                for(var i = 0; i< datasource.triggers.length;i++){
                
                  var solverCopy = angular.copy(datasource.triggers[i]);
                  solverCopy.params.filter = urlParamService.generateFiltersForGadgetId(datasource.triggers[i].emitTo);
                  for(var index in datasource.triggers[i].params.filter){
                    var bundleFilters = datasource.triggers[i].params.filter[index].data;
                    for(var indexB in bundleFilters){
                      solverCopy.params.filter.push(bundleFilters[indexB]);
                    }
                  }
                  if(typeof datasource.triggers[i].isActivated === "undefined" || datasource.triggers[i].isActivated){
                    console.log("sendAndSubscribe",solverCopy);
                    socketService.sendAndSubscribe({"msg":fromTriggerToMessage(solverCopy,datasource.name),id: angular.copy(datasource.triggers[i].emitTo), type:"refresh", callback: vm.emitToTargets});
                  }
                }
              },datasource.refresh * 1000, 0, true, datasource
            );
            vm.poolingDatasources[datasource.name].intervalId = intervalId;
          }
        }
        else{//Streaming datasource
       
        }
      }


vm.getDataFromDataSource = function(datasource,callback){

   socketService.sendAndSubscribe({"msg":fromTriggerToMessage(datasource.triggers[0],datasource.name),id: angular.copy(datasource.triggers[0].emitTo), type:"refresh", callback: callback});

}

     




      
      function fromTriggerToMessage(trigger,dsname){
        var baseMsg = trigger.params;
        baseMsg.ds = dsname;
        baseMsg.dashboard = vm.dashboard;
        return baseMsg;
      }

      vm.emitToTargets = function(id,name,data){
        //pendingDatasources
        $rootScope.$broadcast(id,
          {
            type: "data",
            name: name,
            data: JSON.parse(data.data)
          }
        );
      }

      vm.registerDatasource = function(datasource){
        vm.poolingDatasources[datasource.name] = datasource;
      }

      vm.registerDatasourceTrigger = function(datasource, trigger){//add streaming too
        if(!(datasource.name in vm.poolingDatasources)){
          vm.poolingDatasources[datasource.name] = datasource;
        }
        vm.poolingDatasources[name].triggers.push(trigger);
        //trigger one shot
      }

      vm.unregisterDatasourceTrigger = function(name,emiter){      
        if(name in vm.pendingDatasources && vm.pendingDatasources[name].triggers.length == 0){
          vm.pendingDatasources[name].triggers = vm.pendingDatasources[name].triggers.filter(function(trigger){return trigger.emitTo!=emiter});

          if(vm.pendingDatasources[name].triggers.length==0){
            delete vm.pendingDatasources[name];
          }
        }
        if(name in vm.poolingDatasources && vm.poolingDatasources[name].triggers.length == 0){
          var trigger = vm.poolingDatasources[name].triggers.filter(function(trigger){return trigger.emitTo==emiter});
          trigger.listeners--;
          if(trigger.listeners==0){
            vm.poolingDatasources[name].triggers = vm.poolingDatasources[name].triggers.filter(function(trigger){return trigger.emitTo!=emiter});
          }

          if(vm.poolingDatasources[name].triggers.length==0){
            $interval.clear(vm.poolingDatasources[datasource.name].intervalId);
            delete vm.poolingDatasources[name];
          }
        }
        if(name in vm.streamingDatasources && vm.streamingDatasources[name].triggers.length == 0){
          vm.streamingDatasources[name].triggers = vm.streamingDatasources[name].triggers.filter(function(trigger){return trigger.emitTo!=emiter});

          if(vm.streamingDatasources[name].triggers.length==0){           
            delete vm.streamingDatasources[name];
          }
        }
      }

      vm.disconnect = function(){
        socketService.disconnect();
      }



    //Create filter 
    vm.buildFilterStt = function(dataEvent){   
      return {
        filter: {
          id: dataEvent.id,
          data: dataEvent.data.map(
            function(f){
              //quotes for string identification
              if(typeof f.value === "string"){                
               // var re = /^([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?$/;
              
              /*  if(f.value.length>4 && re.test(f.value)){
                  f.value = "\'" + f.value + "\'"
                }  */
                if(f.op !== "IN" && f.value.indexOf("'")<0){
                  f.value = "\'" + f.value + "\'"
                }
              }
              return {
                field: f.field,
                op: f.op,
                exp: f.value
              }
            }
          )
        } , 
        group:[], 
        project:[]
      }
    }
  }
})(); 
