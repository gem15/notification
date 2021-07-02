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
 *  Список уведомлений
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseFtp {

//	private int ftpId;

	//	/**
//	 *  Код события из справочника sv_hvoc
//	 */
//	private String voc;
	private int vn;
	private String path = "/";
	private String queryMaster;
	private String queryDetails;
	private String alias;
	private String direction;
	private String orderType;
}