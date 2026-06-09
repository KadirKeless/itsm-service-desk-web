package com.example.itsm_final.controller;

import com.example.itsm_final.dto.AdminCreateUserDto;
import com.example.itsm_final.dto.AdminResetPasswordDto;
import com.example.itsm_final.dto.AdminUpdateUserDto;
import com.example.itsm_final.dto.ApiResult;
import com.example.itsm_final.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin kullanici islemleri — sayfa yenilemeden AJAX ile calisir.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminApiController {

    private final UserService userService;

    public AdminApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResult> activate(@PathVariable Integer id,
                                              @RequestParam(required = false) Integer roleId,
                                              @RequestParam(required = false) Integer departmentId) {
        try {
            userService.activateUser(id, roleId, departmentId);
            return ResponseEntity.ok(ApiResult.ok("Kullanıcı aktifleştirildi."));
        } catch (UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/freeze")
    public ResponseEntity<ApiResult> freeze(@PathVariable Integer id) {
        try {
            userService.freezeUser(id);
            return ResponseEntity.ok(ApiResult.ok("Kullanıcı donduruldu."));
        } catch (UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<ApiResult> delete(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResult.ok("Kullanıcı silindi."));
        } catch (UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    @PostMapping("/bulk/activate")
    public ResponseEntity<ApiResult> bulkActivate(@RequestParam("userIds") List<Integer> userIds,
                                                  @RequestParam(required = false) Integer roleId,
                                                  @RequestParam(required = false) Integer departmentId) {
        try {
            int count = userService.bulkActivateUsers(userIds, roleId, departmentId);
            return ResponseEntity.ok(ApiResult.ok(count + " kullanıcı aktifleştirildi."));
        } catch (UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    @PostMapping("/bulk/freeze")
    public ResponseEntity<ApiResult> bulkFreeze(@RequestParam("userIds") List<Integer> userIds) {
        try {
            int count = userService.bulkFreezeUsers(userIds);
            return ResponseEntity.ok(ApiResult.ok(count + " kullanıcı donduruldu."));
        } catch (UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    @PostMapping("/bulk/delete")
    public ResponseEntity<ApiResult> bulkDelete(@RequestParam("userIds") List<Integer> userIds) {
        try {
            int count = userService.bulkDeleteUsers(userIds);
            return ResponseEntity.ok(ApiResult.ok(count + " kullanıcı silindi."));
        } catch (UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResult> create(@Valid @ModelAttribute AdminCreateUserDto dto,
                                            BindingResult bindingResult,
                                            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(fromBindingResult(bindingResult));
        }
        try {
            var created = userService.adminCreateUser(dto);
            if (profilePicture != null && !profilePicture.isEmpty()) {
                userService.updateProfilePicture(created.getId(), profilePicture);
            }
            return ResponseEntity.ok(ApiResult.ok("Yeni kullanıcı oluşturuldu."));
        } catch (UserService.EmailAlreadyExistsException | UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<ApiResult> resetPassword(@PathVariable Integer id,
                                                   @Valid @ModelAttribute AdminResetPasswordDto dto,
                                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(fromBindingResult(bindingResult));
        }
        try {
            userService.adminResetPassword(id, dto);
            return ResponseEntity.ok(ApiResult.ok("Kullanıcı şifresi güncellendi."));
        } catch (UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/edit")
    public ResponseEntity<ApiResult> update(@PathVariable Integer id,
                                          @Valid @ModelAttribute AdminUpdateUserDto dto,
                                          BindingResult bindingResult,
                                          @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
                                          @RequestParam(value = "removeProfilePicture", defaultValue = "false") boolean removeProfilePicture) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(fromBindingResult(bindingResult));
        }
        try {
            if (removeProfilePicture) {
                userService.removeProfilePicture(id);
            } else if (profilePicture != null && !profilePicture.isEmpty()) {
                userService.updateProfilePicture(id, profilePicture);
            }
            userService.updateUser(id, dto);
            return ResponseEntity.ok(ApiResult.ok("Kullanıcı güncellendi."));
        } catch (UserService.BusinessException ex) {
            return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
        }
    }

    private ApiResult fromBindingResult(BindingResult bindingResult) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        StringBuilder detail = new StringBuilder();
        bindingResult.getFieldErrors().forEach(fe -> {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
            if (fe.getDefaultMessage() != null && !fe.getDefaultMessage().isBlank()) {
                if (detail.length() > 0) {
                    detail.append('\n');
                }
                detail.append(fe.getDefaultMessage());
            }
        });
        String message = detail.length() > 0
                ? detail.toString()
                : "Form bilgilerinde hata var. Lütfen kontrol edip tekrar deneyiniz.";
        return ApiResult.validation(message, fieldErrors);
    }
}