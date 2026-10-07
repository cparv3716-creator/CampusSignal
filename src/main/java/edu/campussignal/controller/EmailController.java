package edu.campussignal.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.campussignal.dto.EmailResponse;
import edu.campussignal.service.EmailReadService;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailController {
    private final EmailReadService emails;

    public EmailController(EmailReadService emails) {
        this.emails = emails;
    }

    @GetMapping
    public List<EmailResponse> list() {
        return emails.list();
    }

    @GetMapping("/{id}")
    public EmailResponse get(@PathVariable long id) {
        return emails.get(id);
    }
}
