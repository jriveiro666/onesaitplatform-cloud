(function () {
  'use strict';

  angular.module('dashboardFramework')
    .component('simpleselectnumberfilter', {
      templateUrl: 'app/components/view/filterComponent/filtersComponents/simpleselectnumberfilter.html',
      controller: FilterController,
      controllerAs: 'vm',
      bindings:{
        idfilter:"<?",
        datasource: "<?",
        config:"=?"      
      }
    });

  /** @ngInject */
  function FilterController($scope) {
    var vm = this;
    //structure config =  [{"type":" ", "field":" ","name":" ","op":" ","typeAction":"","initialFilter":""}]
   
    vm.$onInit = function () {

    vm.options = parseOptions(vm.config.data.options);
    vm.optionsSelected = parseFloat(vm.config.data.optionsSelected);

      vm.vue = new Vue({
        el: '#'+vm.config.htmlId,
        data: function() {

          return {
            dynamicValidateForm: {             
              inputName: vm.config.name,
              inputId: vm.idfilter,
              options:vm.options,
              optionsSelected:vm.optionsSelected
            }
          }
        },
        methods: {
          valueChange : function(optionsSelected) {
            $scope.$apply(function() {
              vm.config.data.optionsSelected = parseFloat(optionsSelected);
            });
          }
        }
      })
    };

    function parseOptions(opts){
      var temp = [];
      if(typeof opts!= 'undefined'){
        for (var index = 0; index < opts.length; index++) {
          temp.push(parseFloat(opts[index]));          
        }
      }
      return temp;
    }
  
  }
})();
