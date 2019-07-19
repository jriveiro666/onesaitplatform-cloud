(function () {
    'use strict';

    angular.module('dashboardFramework')
      .component('synopticeditor', {
        templateUrl: 'app/components/view/synopticEditorComponent/synopticEditor.html',
        controller: SynopticEditorController,
        controllerAs: 'vm',
        bindings: {          
          synoptic: "=?",
          config: "<?",
          dashboardheader: "<?" 
        }
      });

    /** @ngInject */
    function SynopticEditorController($rootScope, $scope, $element, $compile, datasourceSolverService, httpService, interactionService, utilsService, urlParamService, filterService) {
      var vm = this;

  

      vm.datasources = new Map();


      
      vm.$onInit = function () {
        //Charge datasources with fields
        httpService.getDatasources().then(
          function(response){
            for(var i=0;i<response.data.length;i++){
              loadFields(response.data[i].identification,response.data[i].id);
            }
          },
          function(e){
            console.log("Error getting datasources: " +  JSON.stringify(e))
          }
        );
        }

        function loadFields(identification,id){
          httpService.getFieldsFromDatasourceId(id).then(
            function(data){
              vm.datasources.set(identification,utilsService.transformJsonFieldsArrays(utilsService.getJsonFields(data.data[0],"", [])))  ;
            }
          )
        }

       vm.initsvgImage = function () {
       
          
        
/**  Conditions example
          vm.conditions = new Map();

          vm.conditions.set('svg_1', {
            identification: 'rectangle',
            datasource: 'helsinki',
            field: 'Helsinki.year',
            class: 'indicator',
            elementAttr: 'fill',
            color: {
              colorOn: '#aaff00',
              colorOff: '#ff0000',
              cutValue: '2'
            }
          });
  
          vm.conditions.set('svg_2', {
            identification: 'circle',
            datasource: 'helsinki',
            field: 'Helsinki.population_women',
            class: 'indicator',
            elementAttr: 'fill',
            color: {
              colorOn: '#aaff00',
              colorOff: '#ff0000',
              cutValue: '113710'
            }
          });
*/  
          //TODO catch window size and put on svg initial image
          //initialize synoptic 
          if(typeof vm.synoptic === 'undefined'){
            vm.synoptic =  {
              svgImage:             
              '<svg width="640" height="480" xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg">'+            
              ' <g class="layer">'+
              ' <title>Layer 1</title>'+
              ' </g>'+
              '</svg>'
              ,
              conditions:[]
             };
          }
          
          vm.editor = $("#synoptic_editor")[0];
          vm.editor.contentWindow.svgEditor.canvas.setSvgString(vm.synoptic.svgImage);
          vm.editor.contentWindow.svgEditor.setConditions(new Map(vm.synoptic.conditions));
          vm.editor.contentWindow.svgEditor.setDatasources(vm.datasources);
       };
       window.initsvgImage = function(){
        vm.initsvgImage();
      }
       
      }
    })();