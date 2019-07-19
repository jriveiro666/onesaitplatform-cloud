var Microservice = Microservice || {};

Microservice.List = (function() {
	"use-strict";
	var mountableModel = $('#table_parameters').find('tr.parameters-model')[0].outerHTML;
	var csrfHeader = headerJson.csrfHeaderName;
	var csrfToken = headerJson.csrfToken;
	var headersObj = {};
	headersObj[csrfHeader] = csrfToken;
	var init = function() {
		 setInterval(reloadMicroserviceTable,10000);
		// Create event
		$('#btn-report-create').on('click', function (e) {
			e.preventDefault(); 
			window.location = '/controlpanel/microservices/create';
		})		
	};
	
	var dtRenderOptions = function (data, type, row) {
		return '<div class="grupo-iconos text-center">'
		+ '<span data-id="' + row.id + '" class="icon-microservice-edit btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.genUpdate+'"><i class="la la-edit font-hg"></i></span>'
		+ '<a target="_blank" href="'+row.deploymentUrl+'"><span data-id="' + row.id + '" class="btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.genView+'"><i class="la la-eye font-hg"></i></span></a>'
		+ '<span data-id="' + row.id + '" class="icon-microservice-trash btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.genDelete+'"><i class="la la-trash font-hg"></i></span>'																											
		+ '</div>';
	};
	
	var dtRenderCICD = function (data, type, row) {
		var div = '<div class="grupo-iconos text-center">'
			+ '<span data-id="' + row.id + '" class="build-btn btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.build+'"><i class="la la-gavel font-hg"></i></span>';																																																			
		if(row.deployed){
			div+='<span data-id="' + row.id + '" data-caas="'+ row.caas +'" class="upgrade-btn btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.upgrade+'"><i class="la la-upload font-hg"></i></span>'																											
			+'<span data-id="' + row.id + '" data-caas="'+ row.caas +'" class="stop-btn btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.stop+'"><i class="la la-stop font-hg"></i></span>'
			+ '</div>';
		}else{
			div+= '<span data-id="' + row.id + '" data-caas="'+ row.caas +'" class="deploy-btn btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.deploy+'"><i class="la la-rocket font-hg"></i></span>'																											
			+ '</div>';
		}
		return div;
		
	};
	
	var dtRenderLinks = function (data,type,row){
		if(data == null)
			return '<div class="text-center" ><span th:text="0" style="display:none" ></span><i class="la la-times-circle-o text-danger  font-hg"></i></div>';
		else
			return '<div class="text-center" ><a href="'+data+'" target="_blank"><span class="link btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.go+'"><i class="la la-link font-hg"></i></span></a></div>';
	}
	
	var dtRenderLinksJenkins = function (data,type,row){
		if(data == null)
			return '<div class="text-center" ><span th:text="0" style="display:none" ></span><i class="la la-times-circle-o text-danger  font-hg"></i></div>';
		else{
			 var html = '<div class="text-center" ><a href="'+data+'" target="_blank"><span class="link btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.go+'"><i class="la la-link font-hg"></i></span></a>';
			if(row.lastBuild != null)
				html +='<span data-id="' + row.id + '" class="btn-jenkins-building btn btn-xs btn-no-border btn-circle btn-outline blue tooltips" data-container="body" data-placement="bottom" data-original-title="'+constants.jenkinsbuilding+'"><i class="fa fa-spinner fa-spin font-hg"></i></span>';
			html+='</div>';
			return html;
		}
	}
	
	function initCompleteCallback(settings, json) {
		
		initTableEvents();
	
	}
	
	
	
	function reloadMicroserviceTable() {
		var oTable = $('.datatable').dataTable();
		reloadDataTable(oTable);
	}
	
	function reloadDataTable(oTable) {		
		oTable.fnClearTable();
		
		oTable.DataTable().ajax.reload(function() {
			Microservice.List.initCompleteCallback()
		}, true);
		
		$('.tooltip').tooltip('destroy');
		$('.tooltips').tooltip();
	}
	

	
	var buildWithParameters = function(){
		var id = $('#current-microservice').val();
		var elements =  $('#table-body').find('tr');
		var parametersArray = [];
		elements.each(function(){
			var name = $(this).find("input[name='name\\[\\]']").val();
			var value = $(this).find("input[name='value\\[\\]']").val();
			var parameter = {"name":name, "value":value};
			parametersArray.push(parameter);
		});
		$('#pulse').attr('class', 'col-md-12');
		$.ajax({
       	 	url : 'jenkins/build/' +id ,  
       	 	headers: headersObj,
       	 	contentType:"application/json; charset=utf-8",
       	 	dataType:"json",
       	 	data: JSON.stringify(parametersArray),
            type : 'POST'
        }).done(function(data) {
			$('#pulse').attr('class', 'col-md-12 hide');
        	reloadMicroserviceTable();
        	$.alert({
				title : 'INFO',
				type : 'blue',
				theme : 'light',
				content : 'Jenkins pipeline was sent to the queue with queue id: '+data
			});
        }).fail(function(error) {
        	
        });
		

	}
	
	var getHosts = function(obj){
		var id = $(obj).data('id');
		var environment = $('#environment').val();
		if(environment != ''){
			$('#pulse').attr('class', 'col-md-12');
			$('#wrapper-deployment-fragment').load('deploy/' +id +'/parameters?hosts=true&environment='+environment, function( response, status, xhr ) {
				  if ( status == "error" ) {
					    var msg = "Sorry but there was an error: ";
					    $( "#error" ).html( msg + xhr.status + " " + xhr.statusText );
				  }else{
					 $('#parametersDeployModal').modal('show');
					 $('#environment').val(environment);
					 
				  }
				
			});
			
		}
	}
	
	var deployWithParameters = function(obj){
		var id = obj.dataset.id;
		var environment = $('#environment').val();
		var worker = $('#worker').val();
		var onesaitServerUrl = $('#onesaitServerUrl').val();
		var dockerImageUrl = $('#dockerImageUrl').val();
		var continueDeploy = true;
		if(environment == ''){
			$('#environment').closest('td').addClass('has-error');
			continueDeploy = false;
		}
		if(worker == ''){
			$('#worker').closest('td').addClass('has-error');
			continueDeploy = false;
		}
		if(dockerImageUrl == ''){
			$('#dockerImageUrl').closest('td').addClass('has-error');
			continueDeploy = false;
		}
		if(continueDeploy){
			$('#environment').closest('td').removeClass('has-error');
			$('#worker').closest('td').removeClass('has-error');
			$('#dockerImageUrl').closest('td').removeClass('has-error');
		}else{
			return;
		}
		var payload = {'environment':environment, 'worker': worker, 'onesaitServerUrl': onesaitServerUrl , 'dockerImageUrl': dockerImageUrl};
		$('#pulse').attr('class', 'col-md-12');
		$.ajax({
       	 	url : 'deploy/' +id  ,  
       	 	headers: headersObj,
       	 	data: payload,
            type : 'POST'
        }).done(function(data) {
        	$('.modal-backdrop').remove()
        	$('#parametersDeployModal').modal('hide');
        	
        	reloadMicroserviceTable();	
        	$.alert({
				title : 'INFO',
				type : 'blue',
				theme : 'light',
				content : 'Microservice deployed'
			});
        }).fail(function(error) {
        	
        });
		

	}
	
	var upgrade = function(obj){
		var id = obj.dataset.id;
		var dockerImageUrl = $('#dockerImageUrlUpgrade').val();
		var continueDeploy = true;
		if(dockerImageUrl == ''){
			$('#dockerImageUrlUpgrade').closest('td').addClass('has-error');
			continueDeploy = false;
		}
		if(continueDeploy)
			$('#dockerImageUrlUpgrade').closest('td').removeClass('has-error');
		else
			return;
		
		
		
		
		var env = {};
		var elements = $(".env-tr");
		elements.each(function(){
			var name = $(this).find("input[name='envName\\[\\]']").val();
			var value = $(this).find("input[name='envValue\\[\\]']").val();
			env[name]=value;
		});
		env = JSON.stringify(env);
		var payload = {'dockerImageUrl':dockerImageUrl, 'env':env};
		$('#pulse').attr('class', 'col-md-12');
		$.ajax({
       	 	url : 'upgrade/' +id  ,  
       	 	headers: headersObj,
       	 	data: payload,
            type : 'POST'
        }).done(function(data) {
			$('#pulse').attr('class', 'col-md-12 hide');
        	$('#parametersUpgradeModal').modal('hide');
        	reloadMicroserviceTable();	
        	$.alert({
				title : 'INFO',
				type : 'blue',
				theme : 'light',
				content : 'Microservice upgraded'
			});
        }).fail(function(error) {
        	
        });
	}
	
	var removeEnvVar = function(obj){
		$(obj).closest('tr').remove();
	}
	
	var addEnvVar = function(){
		var tr = '<tr class="env-tr">'+
				'<td>'+
					'<input type="text" name="envName[]"   class="form-control" placeholder="ENV VAR Name"/>'+
				'</td>'+
				'<td>'+
					'<input type="text" name="envValue[]"  class="form-control" placeholder="ENV VAR Value"/>'+
				'</td>'+
				'<td class="text-center">'+
					'<div class="btn btn-outline  btn-sm blue tooltips btn-add-env" data-container="body" data-placement="top" data-original-title="Environment Variable" onclick="Microservice.List.removeEnvVar(this)">'+
						'<input type="checkbox" id="groupby_check" style="display:none; margin:0px"/><i class="fa fa-minus"></i>'+
					'</div>'+
				'</td>'+
			'</tr>';
		
		$('#table_deployment_parameters tbody').append(tr);
	}
	
	function initTableEvents() {
		$('.tooltips').tooltip();
		$('.build-btn').each(function() {
			$(this).on('click', function (e) {
				e.preventDefault(); 
				var id = $(this).data('id');
				 $.ajax({
			       	 	url : 'jenkins/parameters/' +id ,  
			            type : 'GET'
			        }).done(function(data) {
			        	var parameters = data;
			        	if(parameters == null || parameters.length == 0)
			        		return;
			        	else{
			        		if ($('#parameters').attr('data-loaded') === 'true'){
			    				$('#table_parameters > tbody').html("");
			    				$('#table_parameters > tbody').append(mountableModel);
			    			}
			        		
			        		$('#table_parameters').mounTable(parameters,{
			    				model: '.parameters-model',
			    				noDebug: false							
			    			});
			        		$('#parameters').removeClass('hide');
			    			$('#parameters').attr('data-loaded',true);
			    			$('#parametersModal').modal('show');
			    			$('#current-microservice').val(id);
			        		
			        	}
			        		
			        	
			        }).fail(function(error) {
			        	
			        });
				
			});
		})
		
		$('.deploy-btn').each(function() {
			$(this).on('click', function (e) {
				e.preventDefault(); 
				var id = $(this).data('id');
				
					$('#wrapper-deployment-fragment').load('deploy/' +id +'/parameters', function( response, status, xhr ) {
					  if ( status == "error" ) {
						    var msg = "Sorry but there was an error: ";
						    $( "#error" ).html( msg + xhr.status + " " + xhr.statusText );
					  }else
						 $('#parametersDeployModal').modal('show');
					
				});
			});
		});
		
		$('.stop-btn').each(function() {
			$(this).on('click', function (e) {
				e.preventDefault(); 
				var id = $(this).data('id');
				$.ajax({
		       	 	url : 'stop/' +id  ,  
		       	 	headers: headersObj,
		            type : 'POST'
		        }).done(function(data) {
		        	$.alert({
						title : 'INFO',
						type : 'blue',
						theme : 'light',
						content : 'Microservice stopped'
					});
		        }).fail(function(error) {
		        	$.alert({
						title : 'ERROR',
						type : 'red',
						theme : 'light',
						content : error
					})
					
				});
			});
		});
		
		$('.btn-jenkins-completed').each(function() {
			$(this).on('click', function (e) {
				e.preventDefault(); 
				var id = $(this).data('id');
				$.ajax({
		       	 	url : 'jenkins/completed/' +id  ,  
		       	 	headers: headersObj,
		            type : 'GET'
		        }).done(function(data) {
		        	$.alert({
						title : 'INFO',
						type : 'blue',
						theme : 'light',
						content : 'Build Finished!'
					});
		        	reloadMicroserviceTable();
		        }).fail(function(error) {
		        	$.alert({
						title : 'ERROR',
						type : 'red',
						theme : 'light',
						content : error
					})
					
				});
			});
		});
		
		$('.upgrade-btn').each(function() {
			$(this).on('click', function (e) {
				e.preventDefault(); 
				var id = $(this).data('id');
				$('#pulse').attr('class', 'col-md-12');
				$('#wrapper-deployment-fragment').load('deploy/' +id +'/parameters?upgrade=true', function( response, status, xhr ) {
				  if ( status == "error" ) {
					    var msg = "Sorry but there was an error: ";
					    $( "#error" ).html( msg + xhr.status + " " + xhr.statusText );
				  }else{
					 $('#parametersUpgradeModal').modal('show');
					 $('#pulse').attr('class', 'col-md-12 hide');
				  }
				
				});
			});
		});
		
		
		$('.icon-microservice-edit').each(function() {
			$(this).on('click', function (e) {
				e.preventDefault(); 
				var id = $(this).data('id');
				window.location = '/controlpanel/microservices/update/' + id;
			});
		});
		

		$('.icon-microservice-trash').each(function() {
			$(this).on('click', function (e) {
				e.preventDefault(); 
				var id = $(this).data('id'); 
				deleteMicroserviceDialog(id);
			});
		});
		
		

		
		
	}
	
	var deleteMicroserviceDialog = function(id) {
		$.confirm({
			icon: 'fa fa-warning',
			title: headerJson.btnEliminar,
			theme: 'light',
			columnClass: 'medium',
			content: constants.deleteContent,
			draggable: true,
			dragWindowGap: 100,
			backgroundDismiss: true,
			closeIcon: true,
			buttons: {
				close: {
					text: headerJson.btnClose,
					btnClass: 'btn btn-sm btn-circle btn-outline blue',
					action: function (){} //GENERIC CLOSE.		
				},
				Ok: {
					text: headerJson.btnEliminar,
					btnClass: 'btn btn-sm btn-circle btn-outline btn-blue',
					action: function() { 
						$.ajax({ 
						    url : id,
						    headers: headersObj,
						    type : 'DELETE'
						}).done(function( result ) {							
							reloadMicroserviceTable();
						}).fail(function( error ) {
						   	alert('TODO: Pasar a un modal. \n\nHa habido un error');
						}).always(function() {
						});
					}											
				}					
			}
		});
	}
	
	// Public API
	return {
		init: init,
		dtRenderOptions: dtRenderOptions,
		dtRenderCICD: dtRenderCICD,
		initCompleteCallback: initCompleteCallback,
		reloadMicroserviceTable: reloadMicroserviceTable,
		buildWithParameters: buildWithParameters,
		dtRenderLinks: dtRenderLinks,
		dtRenderLinksJenkins: dtRenderLinksJenkins,
		deployWithParameters: deployWithParameters,
		upgrade: upgrade,
		addEnvVar: addEnvVar,
		removeEnvVar: removeEnvVar,
		getHosts: getHosts
		
	};
	
})();

$(document).ready(function() {	
	
	Microservice.List.init();

});
