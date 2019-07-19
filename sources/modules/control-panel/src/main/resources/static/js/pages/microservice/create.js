var Microservice = Microservice || {};


Microservice.Create = (function(){
	"use-strict";

	
	var init = function() {
		
		
		$('#project-structure').hide();
		$("#btn-cancel").on('click', function (e) {
			e.preventDefault();
			window.location = '/controlpanel/reports/list';
		});
		
		$("#checkbox-publish-gitlab").on('change', function(){
			if( $(this).is(':checked'))
				$('#project-structure').hide();
			else
				$('#project-structure').show();
		});
		
		if(exists.gitlab){
			$('#gitlab-configuration').hide();
			$("#checkbox-gitlab-default").on('change', function(){
				if( $(this).is(':checked'))
					$("#gitlab-configuration").hide();
				else
					$("#gitlab-configuration").show();
			});
		}
		if(exists.jenkins){
			$('#jenkins-configuration').hide();
			$("#checkbox-jenkins-default").on('change', function(){
				if( $(this).is(':checked'))
					$("#jenkins-configuration").hide();
				else
					$("#jenkins-configuration").show();
			});
		}
		if(exists.caas){
			$('#caas-configuration').hide();
			$("#checkbox-caas-default").on('change', function(){
				if( $(this).is(':checked'))
					$("#caas-configuration").hide();
				else
					$("#caas-configuration").show();
			});
		}
		
		$('#template').on('change', function(){
			if($(this).val() != 'IOT_CLIENT_ARCHETYPE' )
				$('#select-ontologies').hide();
			else
				$('#select-ontologies').show();
		})
	
		
		
		
		handleValidation();
	}
	
	function setHiddenInputs(){
		$("#createGitlab").val($("#checkbox-publish-gitlab").is(':checked'));
		if(exists.gitlab)
			$("#defaultGitlab").val($("#checkbox-gitlab-default").is(':checked'));
		if(exists.jenkins)
			$("#defaultJenkins").val($("#checkbox-jenkins-default").is(':checked'));
		if(exists.caas)
			$("#defaultCaaS").val($("#checkbox-caas-default").is(':checked'));
	
	}
	
	function submitForm($form, action, method) {
		$form[0].submit();
	}
	
	var handleValidation = function() {
        var $form = $('#form-microservice');
        var $error = $('.alert-danger');
        var $success = $('.alert-success');
		// set current language
		// TODO: Analizar -> currentLanguage = dashboardCreateReg.language || LANGUAGE;
        
        $form.validate({
            errorElement: 'span', 
            errorClass: 'help-block help-block-error',
            focusInvalid: false, 
            ignore: ":hidden:not('.selectpicker, .hidden-validation')", 
			lang: currentLanguage,
            rules: {				
                identification: { required: true }
            },
            invalidHandler: function(event, validator) {
                $success.hide();
                $error.show();
                App.scrollTo($error, -200);
            },
            highlight: function(element) { // hightlight error inputs
                $(element).closest('.form-group').addClass('has-error'); 
            },
            unhighlight: function(element) { // revert the change done by hightlight
                $(element).closest('.form-group').removeClass('has-error');
            },
            success: function(label) {
                label.closest('.form-group').removeClass('has-error');
            },			
            submitHandler: function(form) { 
            	setHiddenInputs();
				$success.show();
				$error.hide();					
				submitForm($form, $('#microservice-save-action').val(), $('#microservice-save-method').val());
			}
        });
    }	
	
	
	return {
		init: init
	};
})();



$(document).ready(function() {	
	
	Microservice.Create.init();
});