package com.minsait.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
	@NotNull
	private String username;
	@JsonInclude(Include.NON_NULL)
	private String password;
	@NotNull
	private String mail;
	@NotNull
	private String fullName;
	@NotNull
	private String role;
	@JsonInclude(Include.NON_NULL)
	private String extraFields;
	@JsonInclude(Include.NON_NULL)
	private byte[] avatar;

}
