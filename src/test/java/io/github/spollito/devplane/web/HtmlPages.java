package io.github.spollito.devplane.web;

import java.io.UnsupportedEncodingException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Parses pages that tests requested, so they can assert on elements and attributes. */
final class HtmlPages {

	private HtmlPages() {
	}

	static Document parse(MvcTestResult result) {
		try {
			return Jsoup.parse(result.getResponse().getContentAsString(), "http://localhost/");
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
