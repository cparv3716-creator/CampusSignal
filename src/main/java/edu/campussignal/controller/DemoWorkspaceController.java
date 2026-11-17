package edu.campussignal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.campussignal.dto.EmailActionStatusRequest;
import edu.campussignal.dto.StudentProfileRequest;
import edu.campussignal.entity.UserAccount;
import edu.campussignal.security.AuthenticatedUserService;
import edu.campussignal.service.DemoWorkspaceService;

/** Authenticated, non-persistent presentation fixtures. These routes never call Gmail or repositories. */
@RestController
@RequestMapping("/api/v1/me/demo")
public class DemoWorkspaceController {
    private final AuthenticatedUserService auth;
    private final DemoWorkspaceService demo;
    public DemoWorkspaceController(AuthenticatedUserService auth, DemoWorkspaceService demo) { this.auth = auth; this.demo = demo; }
    @GetMapping public Object workspace(HttpServletRequest request) { return demo.workspace(user(request)); }
    @PutMapping("/profile") public Object profile(@RequestHeader(value = "X-CampusSignal-CSRF", required = false) String csrf, HttpServletRequest request, @Valid @RequestBody StudentProfileRequest body) { return demo.saveProfile(mutate(request, csrf), body); }
    @PostMapping("/reset") public Object reset(@RequestHeader(value = "X-CampusSignal-CSRF", required = false) String csrf, HttpServletRequest request) { return demo.reset(mutate(request, csrf)); }
    @GetMapping("/emails/{id}/content") public Object content(@PathVariable long id, HttpServletRequest request) { return demo.content(user(request), id); }
    @GetMapping("/emails/{id}/action-candidate") public ResponseEntity<?> candidate(@PathVariable long id, HttpServletRequest request) { return demo.candidate(user(request), id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build()); }
    @PostMapping("/emails/{id}/actions") public Object createAction(@PathVariable long id, @RequestHeader(value = "X-CampusSignal-CSRF", required = false) String csrf, HttpServletRequest request) { return demo.createAction(mutate(request, csrf), id); }
    @PatchMapping("/actions/{id}/status") public Object updateAction(@PathVariable long id, @RequestHeader(value = "X-CampusSignal-CSRF", required = false) String csrf, HttpServletRequest request, @Valid @RequestBody EmailActionStatusRequest body) { return demo.updateAction(mutate(request, csrf), id, body.status()); }
    private UserAccount user(HttpServletRequest request) { return auth.current(request); }
    private UserAccount mutate(HttpServletRequest request, String csrf) { return auth.currentForMutation(request, csrf); }
}
