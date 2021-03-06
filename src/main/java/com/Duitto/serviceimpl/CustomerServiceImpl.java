package com.Duitto.serviceimpl;

import java.util.HashMap;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.Duitto.config.JwtTokenUtil;
import com.Duitto.converter.UserConverter;
import com.Duitto.dto.Mail;
import com.Duitto.dto.UserDto;
import com.Duitto.model.ConfirmationTokenModel;
import com.Duitto.model.CustomerRegistrationModel;
import com.Duitto.model.JWTTOkenModel;
import com.Duitto.model.ReserveUserNameModel;
import com.Duitto.model.ReservedUsernameListModel;
import com.Duitto.repository.ConfirmationTokenRepository;
import com.Duitto.repository.CustomerRepository;
import com.Duitto.repository.JWTTokenRepository;
import com.Duitto.repository.ReservedUserNameListRepository;
import com.Duitto.service.CustomerService;
import com.Duitto.service.MailService;
import com.Duitto.utility.MailUtils;
import com.Duitto.utility.MethodsUtility;

@Service("CustomerService")
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	CustomerRepository custRepos;

	@Autowired
	ConfirmationTokenRepository confirmationTokenRepository;

	@Autowired
	JWTTokenRepository jwtTokenRepository;

	@Autowired
	MailService mailService;
	
	@Autowired
	ReservedUserNameListRepository reservedUnameRepos;

	@Override
	public HashMap<String, Object> registerCustomer(UserDto customerDto) {
		HashMap<String, Object> map = new HashMap();

		try {
			if (customerDto.getId() != null) {

				Optional<CustomerRegistrationModel> customer = custRepos.findById(customerDto.getId());
				if(customer.isPresent()) {
					CustomerRegistrationModel customermodel = UserConverter.dtoToEntityWithIdForUpdate(customerDto);
					map.put("data", custRepos.save(customermodel));
					map.put("message", "Customer Updated!");
					map.put("status", true);
				}
 				
			} else {
				Optional<CustomerRegistrationModel> user = custRepos.findOneByEmail(customerDto.getEmail());
				if (user.isPresent()) {
					map.put("message", "Email already registered!");
					map.put("status", false);
				} else {
					map.put("data", custRepos.save(UserConverter.dtoToEntity(customerDto)));
					map.put("status", true);
					ConfirmationTokenModel confirmationToken = new ConfirmationTokenModel(customerDto.getEmail(),
							MethodsUtility.getCurrentTimestamp());
					saveConfirmationToken(confirmationToken);
					Mail mail = MailUtils.emailVerification(confirmationToken, customerDto.getEmail());
					mailService.sendEmail(mail);
				}

			}
		} catch (Exception e) {
			map.put("message", e.getMessage());
			map.put("status", false);
			e.printStackTrace();
		}

		return map;
	}

	@Override
	public HashMap<String, Object> customerSignIn(UserDto customer) {
		HashMap<String, Object> map = new HashMap();
		Optional<CustomerRegistrationModel> option = custRepos.findOneByEmail(customer.getEmail());
		try {
			if (option.isPresent()) {

				String token = new JwtTokenUtil().generateToken(option.get());
				JWTTOkenModel jwtToken = new JWTTOkenModel(token, MethodsUtility.getCurrentTimestamp(),
						customer.getEmail());
				saveJWTToken(jwtToken);
				UserDto dto = UserConverter.entityToDto(option.get());
				map.put("data", dto);
				Mail mail = MailUtils.emailAtLogin(dto, token);
				mailService.sendEmail(mail);
				map.put("token", token);
				map.put("status", true);
			}

			else {
				map.put("mesage", "No Customer Found");
				map.put("status", false);
			}
		} catch (Exception e) {
			map.put("mesage", e.getMessage());
			map.put("status", false);
		}

		return map;
	}

	@Override
	public void saveConfirmationToken(ConfirmationTokenModel confirmationToken) {
		confirmationTokenRepository.save(confirmationToken);
	}

	@Override
	public void saveJWTToken(JWTTOkenModel jwtToken) {
		jwtTokenRepository.save(jwtToken);
	}

	@Override
	public HashMap<String, Object> confirmUser(ConfirmationTokenModel confirmationToken) {
		HashMap<String, Object> map = new HashMap();
		boolean status = false;
		try {

			String cusEmail = confirmationToken.getCusEmail();
			final CustomerRegistrationModel customer = custRepos.findByEmail(cusEmail);
			if (customer == null) {
				throw new UsernameNotFoundException(cusEmail);
			}
			customer.setEnabled(true);
			custRepos.save(customer);

			status = deleteConfirmationToken(confirmationToken.getId());
			if (status == true) {
				map.put("status", true);
				map.put("message", "success");
			} else {
				map.put("status", false);
				map.put("message", "Something went wrong in token deletion.");
			}
		} catch (Exception e) {
			map.put("status", false);
			map.put("message", e.getMessage());
		}

		return map;

	}

	@Override
	public boolean deleteConfirmationToken(Long pkId) {
		boolean status = false;
		try {
			confirmationTokenRepository.deleteById(pkId);
			status = true;
		} catch (Exception e) {
			status = false;
		}
		return status;

	}

	@Override
	public Optional<ConfirmationTokenModel> findByConfirmationToken(String token) {

		return confirmationTokenRepository.findByConfirmationToken(token);
	}

	@Override
	public HashMap<String, Object> loginVerification(String token) {
		HashMap<String, Object> map = new HashMap();
		boolean status = false;
		try {
			Optional<JWTTOkenModel> jwtToken = jwtTokenRepository.findByJwtToken(token);
			if (jwtToken.isPresent()) {
				Optional<CustomerRegistrationModel> customer = custRepos.findOneByEmail(jwtToken.get().getCusEmail());
				map.put("data", customer.get());
				map.put("status", true);
				map.put("token", token);
				// deleteJWTToken(jwtToken.get().getId());
			} else {
				map.put("message", "Link Expired.");
				map.put("status", false);
			}
		} catch (Exception e) {
			map.put("status", false);
			map.put("message", e.getMessage());
			e.printStackTrace();
		}
		return map;
	}

	@Override
	public boolean deleteJWTToken(Long Id) {
		boolean status = false;
		try {
			jwtTokenRepository.deleteById(Id);
			status = true;
		} catch (Exception e) {
			status = false;
		}
		return status;
	}

	@Override
	public HashMap<String, Object> userNameVerification(String json) {
		HashMap<String, Object> map = new HashMap();
		JSONObject jsonObj = new JSONObject(json);
		try {
			String uname = jsonObj.getString("uname");
			Optional<CustomerRegistrationModel> custmodel = custRepos.findByUserName(uname);
			Optional<ReserveUserNameModel> reserveduname = reservedUnameRepos.findbyUserName(uname);
			if(reserveduname.isPresent()) {
				map.put("reserveduname",reserveduname.get().getReservedUserName());
				map.put("isAvailable",reserveduname.get().getIsAvailable());
				map.put("isRequest",reserveduname.get().getIsRequest());
				map.put("uname","NA");
				map.put("status", true);
			}else if(custmodel.isPresent()) {
				map.put("reserveduname","NA");
				map.put("uname",custmodel.get().getUserName());
				map.put("status", true);
			}else {
				map.put("reserveduname","NA");
				map.put("uname","NA");
				map.put("message", "not available");
				map.put("status", false);
			}
			
		} catch (Exception e) {
			map.put("message", e.getMessage());
			e.printStackTrace();
			map.put("status", false);
		}
		return map;
	}

	@Override
	public HashMap<String, Object> requestToAdminForReservedUserName(String json) {
		HashMap<String, Object> map = new HashMap();
		JSONObject jsonObj = new JSONObject(json);
		try {
			
			Optional<ReserveUserNameModel> model = reservedUnameRepos.findbyUserName(jsonObj.getString("reservedUserName"));
			//Optional<CustomerRegistrationModel> customer = custRepos.findById(jsonObj.getLong("custId"));
			if(model.isPresent()) {
				//model.get().setCustomer(customer.get());
				model.get().setCustomerId(jsonObj.getLong("custId"));
				model.get().setIsRequest(1);
				model.get().setReason(jsonObj.getString("reason"));
				reservedUnameRepos.save(model.get());
				Mail mail = MailUtils.reservedUsernameRequestToAdmin();
				mailService.sendEmail(mail);
				map.put("message","request send to admin");
				map.put("status", true);
			}
			
		} catch (Exception e) {
			map.put("message",e.getMessage());
			map.put("status", false);
			e.printStackTrace();
		}
		return map;
	}

}
