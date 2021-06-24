/**
 * "Visual Paradigm: DO NOT MODIFY THIS FILE!"
 * 
 * This is an automatic generated file. It will be regenerated every time 
 * you generate persistence class.
 * 
 * Modifying its content may cause the program not work, or your work may lost.
 */

/**
 * Licensee: Gennady
 * License Type: Purchased
 */
package com.severtrans.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  FTP connection properties
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ftp {

	private int Id;
	
	private String login;
	
	private String password;
	
	private String hostname;
	
	private int port = 21;
	
}
