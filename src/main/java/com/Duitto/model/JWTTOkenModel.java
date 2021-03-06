package com.Duitto.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@SequenceGenerator(name = "seqToken", initialValue = 1212121)
@Table(name = "sh_JWT_tokens")
public class JWTTOkenModel {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqToken")
	@Column(name = "id", unique = true, nullable = false)
	private Long id;

	private String jwtToken;

	private long createdDate;

	private String cusEmail;

	public JWTTOkenModel(String jwtToken, long createdDate, String cusEmail) {

		this.jwtToken = jwtToken;
		this.createdDate = createdDate;
		this.cusEmail = cusEmail;
	}



	public JWTTOkenModel() {
		super();
		// TODO Auto-generated constructor stub
	}



	public String getJwtToken() {
		return jwtToken;
	}



	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}



	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(long createdDate) {
		this.createdDate = createdDate;
	}

	public String getCusEmail() {
		return cusEmail;
	}

	public void setCusEmail(String cusEmail) {
		this.cusEmail = cusEmail;
	}

}
