package com.domcouch.formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Remaining built-in handlers: conversion, type checking, existence,
 * list ops, control flow, security, boolean, validation, error,
 * data conversion, placeholders, document lifecycle, folders etc.
 */
final class MiscHandlers {
    private MiscHandlers() {}

    static void register(java.util.Map<String, FunctionHandler> functions) {
        functions.put("TEXT", (ev, args, ctx) -> { Object val = ev.eval(args.get(0), ctx); String fmt = args.size() > 1 ? Evaluator.toString(ev.eval(args.get(1), ctx)) : null; List<Object> s = Evaluator.toList(val); List<Object> r = new ArrayList<>(); for (Object o : s) { if (o instanceof Number n && fmt != null && !fmt.isEmpty()) r.add(Evaluator.formatNumber(n.doubleValue(), fmt)); else if (fmt != null && !fmt.isEmpty() && (fmt.startsWith("D")||fmt.startsWith("T")||fmt.startsWith("S"))) r.add(Evaluator.formatDate(Evaluator.toString(o), fmt)); else r.add(Evaluator.toString(o)); } return r.size() == 1 ? r.get(0) : r; });
        functions.put("TEXTTONUMBER", (ev, args, ctx) -> { List<Object> s = Evaluator.toList(ev.eval(args.get(0), ctx)); List<Object> r = new ArrayList<>(); for (Object o : s) { String str = Evaluator.toString(o).trim(); if (str.isEmpty()) { r.add(0.0); continue; } double d = 0.0; try { d = Double.parseDouble(str); } catch (NumberFormatException e) { int end = 0; if (end < str.length() && (str.charAt(end)=='+'||str.charAt(end)=='-')) end++; while (end < str.length() && (Character.isDigit(str.charAt(end))||str.charAt(end)=='.')) end++; if (end>0&&(str.charAt(0)=='-'||str.charAt(0)=='+'||Character.isDigit(str.charAt(0)))) { try { d = Double.parseDouble(str.substring(0,end)); } catch (NumberFormatException e2) {} } } r.add(d); } return r.size()==1 ? r.getFirst() : r; });
        functions.put("ISNUMBER", (ev, args, ctx) -> { Object v = ev.eval(args.getFirst(), ctx); if (v instanceof Number) return 1.0; if (v instanceof List<?> l) { for (Object e : l) if (!(e instanceof Number)) return 0.0; return l.isEmpty()?0.0:1.0; } return 0.0; });
        functions.put("ISTEXT", (ev, args, ctx) -> { Object v = ev.eval(args.getFirst(), ctx); if (v instanceof String) return 1.0; if (v instanceof List<?> l) { for (Object e : l) if (!(e instanceof String)) return 0.0; return l.isEmpty()?0.0:1.0; } return 0.0; });
        functions.put("ISAUTHOR", (ev, args, ctx) -> 1.0);
        functions.put("ISAVAILABLE", (ev, args, ctx) -> { String n; if (args.getFirst() instanceof Expr.Variable v) n = v.name(); else n = Evaluator.toString(ev.eval(args.getFirst(), ctx)); return Evaluator.boolToNum(ctx.resolve(n) != null); });
        functions.put("ISUNAVAILABLE", (ev, args, ctx) -> { String n; if (args.getFirst() instanceof Expr.Variable v) n = v.name(); else n = Evaluator.toString(ev.eval(args.getFirst(), ctx)); return Evaluator.boolToNum(ctx.resolve(n) == null); });
        functions.put("ISNEWDOC", (ev, args, ctx) -> { try { return Evaluator.boolToNum(ctx.getDocumentUNID().isEmpty()); } catch (ContextNotSupportedException e) { return 1.0; } });
        functions.put("ISRESPONSEDOC", (ev, args, ctx) -> { Object p = ctx.resolve("PARENTUNID"); return Evaluator.boolToNum(p != null && !Evaluator.toString(p).isEmpty()); });
        functions.put("NOTEID", (ev, args, ctx) -> { try { return "NT"+ctx.getDocumentUNID().substring(0,Math.min(8,ctx.getDocumentUNID().length())); } catch (ContextNotSupportedException e) { return ""; } });
        functions.put("INHERITEDDOCUMENTUNIQUEID", (ev, args, ctx) -> ctx.resolve("PARENTUNID")!=null?Evaluator.toString(ctx.resolve("PARENTUNID")):"");
        functions.put("AUTHOR", (ev, args, ctx) -> ctx.resolve("AUTHORS")!=null?ctx.resolve("AUTHORS"):"");
        functions.put("ATTACHMENTS", (ev, args, ctx) -> { try { return (double)ctx.getAttachmentCount(); } catch (ContextNotSupportedException e) { return 0.0; } });
        functions.put("ELEMENTS", (ev, args, ctx) -> { Object v = ev.eval(args.getFirst(), ctx); if (v instanceof List l) return (double)l.size(); if (v==null||(v instanceof String s&&s.isEmpty())) return 0.0; return 1.0; });
        functions.put("COUNT", (ev, args, ctx) -> { Object v = ev.eval(args.get(0), ctx); if (v instanceof List l) return l.isEmpty()?1.0:(double)l.size(); return 1.0; });
        functions.put("ISMEMBER", (ev, args, ctx) -> { Object val = ev.eval(args.get(0),ctx), list = ev.eval(args.get(1),ctx); List<Object> l2 = Evaluator.toList(list); if (val instanceof List<?> l1) { for (Object e : l1) if (!l2.contains(e)) return 0.0; return l1.isEmpty()?0.0:1.0; } return Evaluator.boolToNum(l2.contains(val)); });
        functions.put("ISNOTMEMBER", (ev, args, ctx) -> { Object val = ev.eval(args.get(0),ctx), list = ev.eval(args.get(1),ctx); List<Object> l2 = Evaluator.toList(list); if (val instanceof List<?> l1) { for (Object e : l1) if (l2.contains(e)) return 0.0; return 1.0; } return Evaluator.boolToNum(!l2.contains(val)); });
        functions.put("REPLACE", (ev, args, ctx) -> { List<Object> s = Evaluator.toList(ev.eval(args.get(0),ctx)), f = Evaluator.toList(ev.eval(args.get(1),ctx)), t = Evaluator.toList(ev.eval(args.get(2),ctx)); List<Object> r = new ArrayList<>(); for (Object src : s) { int idx = f.indexOf(src); r.add(idx>=0&&idx<t.size()?t.get(idx):src); } return r.size()==1?r.getFirst():r; });
        functions.put("MAX", (ev, args, ctx) -> { double max = Double.NEGATIVE_INFINITY; for (Expr arg : args) for (Object o : Evaluator.toList(ev.eval(arg, ctx))) max = Math.max(max, Evaluator.toNumber(o)); return max==(int)max?(double)(int)max:max; });
        functions.put("MIN", (ev, args, ctx) -> { double min = Double.POSITIVE_INFINITY; for (Expr arg : args) for (Object o : Evaluator.toList(ev.eval(arg, ctx))) min = Math.min(min, Evaluator.toNumber(o)); return min==(int)min?(double)(int)min:min; });
        functions.put("SUM", (ev, args, ctx) -> { double sum = 0; for (Expr arg : args) for (Object o : Evaluator.toList(ev.eval(arg, ctx))) sum += Evaluator.toNumber(o); return sum==(int)sum?(double)(int)sum:sum; });
        functions.put("MODULO", (ev, args, ctx) -> Evaluator.map2(ev, args, ctx, (a,b)->{if(b==0)return 0.0;double r=a%b;return r<0?r+Math.abs(b):r;}));
        functions.put("SIGN", (ev, args, ctx) -> { double v = Evaluator.toNumber(ev.eval(args.getFirst(),ctx)); return v>0?1.0:v<0?-1.0:0.0; });
        functions.put("SUBSET", (ev, args, ctx) -> { List<Object> s = Evaluator.toList(ev.eval(args.get(0),ctx)); int n = (int)Evaluator.toNumber(ev.eval(args.get(1),ctx)); List<Object> r = new ArrayList<>(); if (n>0) for (int i=0;i<n&&i<s.size();i++) r.add(s.get(i)); else for (int i=s.size()-1;i>=s.size()+n&&i>=0;i--) r.addFirst(s.get(i)); return r.isEmpty()?"":r.size()==1?r.getFirst():r; });
        functions.put("UNIQUE", (ev, args, ctx) -> { List<Object> s = Evaluator.toList(ev.eval(args.getFirst(),ctx)); java.util.LinkedHashSet<Object> seen = new java.util.LinkedHashSet<>(); for (Object o : s) seen.add(Evaluator.toString(o)); List<Object> r = new ArrayList<>(seen); return r.isEmpty()?"":r.size()==1?r.getFirst():r; });
        functions.put("MEMBER", (ev, args, ctx) -> { Object n = ev.eval(args.get(0),ctx); List<Object> h = Evaluator.toList(ev.eval(args.get(1),ctx)); for (int i=0;i<h.size();i++) if (Evaluator.toString(n).equals(Evaluator.toString(h.get(i)))) return (double)(i+1); return 0.0; });
        functions.put("IMPLODE", (ev, args, ctx) -> { List<Object> s = Evaluator.toList(ev.eval(args.get(0),ctx)); String sep = args.size()>1?Evaluator.toString(ev.eval(args.get(1),ctx)):" "; return s.isEmpty()?"":String.join(sep, s.stream().map(Evaluator::toString).toList()); });
        functions.put("SORT", (ev, args, ctx) -> { List<Object> s = new ArrayList<>(Evaluator.toList(ev.eval(args.get(0),ctx))); s.sort((a,b)->Evaluator.toString(a).compareTo(Evaluator.toString(b))); return s.isEmpty()?"":s.size()==1?s.get(0):s; });
        functions.put("IF", (ev, args, ctx) -> { int n = args.size(); if (n==0) return ""; if (n==1) { ev.eval(args.getFirst(),ctx); return ""; } for (int i=0;i+1<n;i+=2) if (Evaluator.isTruthy(ev.eval(args.get(i),ctx))) return ev.eval(args.get(i+1),ctx); if (n%2==1) return ev.eval(args.get(n-1),ctx); return ""; });
        functions.put("DO", (ev, args, ctx) -> { Object last = ""; for (Expr arg : args) last = ev.eval(arg, ctx); return last; });
        functions.put("RETURN", (ev, args, ctx) -> { throw new Evaluator.ReturnValue(ev.eval(args.getFirst(), ctx)); });
        functions.put("ISNULL", (ev, args, ctx) -> { Object v = ev.eval(args.get(0),ctx); return Evaluator.boolToNum(v==null||"".equals(Evaluator.toString(v))); });
        functions.put("ISVALID", (ev, args, ctx) -> { try { return Evaluator.boolToNum(ctx.isDocumentValid()); } catch (ContextNotSupportedException e) { return 1.0; } });
        functions.put("ALL", (ev, args, ctx) -> 1.0);
        functions.put("TRUE", (ev, args, ctx) -> 1.0);
        functions.put("FALSE", (ev, args, ctx) -> 0.0);
        functions.put("SUCCESS", (ev, args, ctx) -> 1.0);
        functions.put("YES", (ev, args, ctx) -> 1.0);
        functions.put("NO", (ev, args, ctx) -> 0.0);
        functions.put("NOTHING", (ev, args, ctx) -> "");
        functions.put("RANDOM", (ev, args, ctx) -> Math.random());
        functions.put("FAILURE", (ev, args, ctx) -> args.isEmpty()?"":Evaluator.toString(ev.eval(args.getFirst(),ctx)));
        functions.put("DELETEFIELD", (ev, args, ctx) -> new Expr.DeleteField(args.isEmpty()?new Expr.Variable(""):args.getFirst()));
        functions.put("ERROR", (ev, args, ctx) -> Evaluator.ERROR_VALUE);
        functions.put("ISERROR", (ev, args, ctx) -> Evaluator.boolToNum(ev.eval(args.getFirst(),ctx)==Evaluator.ERROR_VALUE));
        functions.put("LIKE", (ev, args, ctx) -> { Object str = ev.eval(args.get(0),ctx), pat = ev.eval(args.get(1),ctx); String ec = args.size()>2?Evaluator.toString(ev.eval(args.get(2),ctx)):null; return Evaluator.boolToNum(Evaluator.anyPairMatch(str, pat, (s,pattern)->{ StringBuilder re = new StringBuilder("^(?i)"); for (int i=0;i<pattern.length();i++) { char c = pattern.charAt(i); if (ec!=null&&!ec.isEmpty()&&pattern.startsWith(ec,i)) { i+=ec.length(); if (i<pattern.length()) re.append(java.util.regex.Pattern.quote(String.valueOf(pattern.charAt(i)))); continue; } if (c=='_') re.append('.'); else if (c=='%') re.append(".*"); else re.append(java.util.regex.Pattern.quote(String.valueOf(c))); } return s.matches(re.append("$").toString()); })); });
        functions.put("IFERROR", (ev, args, ctx) -> { try { return ev.eval(args.get(0),ctx); } catch (Exception e) { return args.size()>1?ev.eval(args.get(1),ctx):""; } });
        functions.put("ISTIME", (ev, args, ctx) -> { List<Object> s = Evaluator.toList(ev.eval(args.get(0),ctx)); for (Object o : s) { if (o instanceof Number) return 0.0; if (Evaluator.parseDateToZoned(Evaluator.toString(o))==null) return 0.0; } return 1.0; });
        functions.put("TEXTTOTIME", (ev, args, ctx) -> { List<Object> s = Evaluator.toList(ev.eval(args.get(0),ctx)); List<Object> r = new ArrayList<>(); for (Object o : s) { java.time.ZonedDateTime z = Evaluator.parseDateToZoned(Evaluator.toString(o)); r.add(z==null?"":Evaluator.DT_FMT.format(z)); } return r.size()==1?r.get(0):r; });
        functions.put("TONUMBER", (ev, args, ctx) -> Evaluator.map1(ev,args,ctx,s->{try{return Evaluator.toNumber(s);}catch(Exception e){return 0.0;}}));
        functions.put("TOTIME", (ev, args, ctx) -> { List<Object> s = Evaluator.toList(ev.eval(args.get(0),ctx)); List<Object> r = new ArrayList<>(); for (Object o : s) { java.time.ZonedDateTime z = Evaluator.parseDateToZoned(Evaluator.toString(o)); r.add(z==null?"":Evaluator.DT_FMT.format(z)); } return r.size()==1?r.get(0):r; });
        functions.put("CLIENTTYPE", (ev, args, ctx) -> "Notes");
        functions.put("DBEXISTS", (ev, args, ctx) -> 1.0);
        functions.put("LANGUAGEPREFERENCE", (ev, args, ctx) -> "EN");
        functions.put("LOCALE", (ev, args, ctx) -> java.util.Locale.getDefault().toString());
        functions.put("KEYWORDS", (ev, args, ctx) -> List.of());
        functions.put("THISNAME", (ev, args, ctx) -> "");
        functions.put("THISVALUE", (ev, args, ctx) -> "");
        functions.put("URLQUERYSTRING", (ev, args, ctx) -> "");
        functions.put("V3USERNAME", (ev, args, ctx) -> ev.getCurrentUserName());
        functions.put("V4USERACCESS", (ev, args, ctx) -> 1.0);
        functions.put("UNAVAILABLE", (ev, args, ctx) -> Evaluator.boolToNum(ctx.resolve(Evaluator.toString(ev.eval(args.get(0),ctx)))==null));
        functions.put("ENVIRONMENT", (ev, args, ctx) -> { try { return ctx.getEnvironmentValue(Evaluator.toString(ev.eval(args.getFirst(),ctx))); } catch (ContextNotSupportedException e) { return ""; } });
        functions.put("REGQUERYVALUE", (ev, args, ctx) -> "");
        functions.put("GETIMCONTACTLISTGROUPNAMES", (ev, args, ctx) -> List.of());
        functions.put("USERNAMELANGUAGE", (ev, args, ctx) -> "EN");
        functions.put("USERNAME", (ev, args, ctx) -> ev.getCurrentUserName());
        functions.put("USERROLES", (ev, args, ctx) -> List.of());
        functions.put("USERNAMESLIST", (ev, args, ctx) -> List.of(ev.getCurrentUserName()));
        functions.put("DOMAIN", (ev, args, ctx) -> { try { return ctx.getDomain(); } catch (ContextNotSupportedException e) { return ""; } });
        functions.put("VERSION", (ev, args, ctx) -> "Domino 14.5 / Couchbase");
        functions.put("DBNAME", (ev, args, ctx) -> { try { return List.of(ctx.getServerName(), ctx.getDatabaseName()); } catch (ContextNotSupportedException e) { return List.of("",""); } });
        functions.put("DBTITLE", (ev, args, ctx) -> { try { return ctx.getDatabaseTitle(); } catch (ContextNotSupportedException e) { return ""; } });
        functions.put("REPLICAID", (ev, args, ctx) -> { try { return ctx.getReplicaID(); } catch (ContextNotSupportedException e) { return ""; } });
        functions.put("SERVERNAME", (ev, args, ctx) -> { try { return ctx.getServerName(); } catch (ContextNotSupportedException e) { return ""; } });
        functions.put("DOCFIELDS", (ev, args, ctx) -> { try { return ctx.getFieldNames(); } catch (ContextNotSupportedException e) { return List.of(); } });
        functions.put("DOCLENGTH", (ev, args, ctx) -> { try { return (double)ctx.getDocumentSize(); } catch (ContextNotSupportedException e) { return 0.0; } });
        functions.put("DOCUMENTUNIQUEID", (ev, args, ctx) -> { try { return ctx.getDocumentUNID(); } catch (ContextNotSupportedException e) { return ""; } });
        functions.put("DOCLOCK", (ev, args, ctx) -> { if (args.isEmpty()) return ""; String kw = Evaluator.toString(ev.eval(args.getFirst(),ctx)); try { return switch(kw){case "LOCK"->Evaluator.boolToNum(ctx.lockDocument());case "UNLOCK"->Evaluator.boolToNum(ctx.unlockDocument());case "STATUS"->ctx.getDocumentLockStatus();case "LOCKINGENABLED"->Evaluator.boolToNum(ctx.isDocumentLockingEnabled());default->"";}; } catch (ContextNotSupportedException e) { return switch(kw){case "LOCK","UNLOCK"->1.0;case "STATUS"->"";case "LOCKINGENABLED"->0.0;default->"";}; } });
        functions.put("DELETEDOCUMENT", (ev, args, ctx) -> { try { ctx.markForDeletion(); return 1.0; } catch (ContextNotSupportedException e) { return 1.0; } });
        functions.put("UNDELETEDOCUMENT", (ev, args, ctx) -> { try { ctx.unmarkForDeletion(); return 1.0; } catch (ContextNotSupportedException e) { return 1.0; } });
        functions.put("HARDDELETEDOCUMENT", (ev, args, ctx) -> { try { ctx.hardDelete(); return 1.0; } catch (ContextNotSupportedException e) { return 1.0; } });
        functions.put("DOCCOMMITTEDLENGTH", (ev, args, ctx) -> { try { return (double)ctx.getDocumentSize(); } catch (ContextNotSupportedException e) { return 0.0; } });
        functions.put("ADDTOFOLDER", (ev, args, ctx) -> { try { ctx.addToFolder(Evaluator.toString(ev.eval(args.getFirst(),ctx))); return 1.0; } catch (ContextNotSupportedException e) { return 1.0; } });
        functions.put("WHICHFOLDERS", (ev, args, ctx) -> { try { return ctx.getFolderNames(); } catch (ContextNotSupportedException e) { return List.of(); } });
        functions.put("NARROW", (ev, args, ctx) -> 1.0);
        functions.put("WIDE", (ev, args, ctx) -> 1.0);
        functions.put("GETFIELD", (ev, args, ctx) -> { Object v = ctx.resolve(Evaluator.toString(ev.eval(args.get(0),ctx))); return v!=null?v:""; });
        functions.put("CHECKFORMULASYNTAX", (ev, args, ctx) -> { String f = Evaluator.toString(ev.eval(args.get(0),ctx)); try { new Parser(Lexer.tokenize(f)).parse(); return "1"; } catch (FormulaParseException e) { return List.of(e.getMessage(),"1",String.valueOf(e.position+1),"1",String.valueOf(e.position+1),"1",f); } });
        functions.put("EVAL", (ev, args, ctx) -> ev.evalExpr(Evaluator.toString(ev.eval(args.get(0),ctx)),ctx));
        functions.put("WHILE", (ev, args, ctx) -> { while (Evaluator.isTruthy(ev.eval(args.get(0),ctx))) for (int i=1;i<args.size();i++) ev.eval(args.get(i),ctx); return 1.0; });
        functions.put("DOWHILE", (ev, args, ctx) -> { do { for (int i=0;i<args.size()-1;i++) ev.eval(args.get(i),ctx); } while (Evaluator.isTruthy(ev.eval(args.get(args.size()-1),ctx))); return 1.0; });
        functions.put("FOR", (ev, args, ctx) -> { if (args.size()<4) return ""; ev.eval(args.get(0),ctx); Object last=1.0; while (Evaluator.isTruthy(ev.eval(args.get(1),ctx))) { for (int i=3;i<args.size();i++) last=ev.eval(args.get(i),ctx); ev.eval(args.get(2),ctx); } return last; });
        functions.put("TRANSFORM", (ev, args, ctx) -> { List<Object> list = Evaluator.toList(ev.eval(args.get(0),ctx)); String vn = Evaluator.toString(ev.eval(args.get(1),ctx)).toUpperCase(); Expr f = args.get(2); List<Object> r = new ArrayList<>(); for (Object elem : list) { FormulaContext ec = new FormulaContext() { @Override public Object resolve(String n) { return n.equals(vn)?elem:ctx.resolve(n); } @Override public void setField(String n, Object v) { ctx.setField(n,v); } @Override public void deleteField(String n) { ctx.deleteField(n); } @Override public List<String> getFieldNames() { return ctx.getFieldNames(); } @Override public String getDocumentUNID() { return ctx.getDocumentUNID(); } @Override public String getDatabaseName() { return ctx.getDatabaseName(); } @Override public String getServerName() { return ctx.getServerName(); } @Override public String getDatabaseTitle() { return ctx.getDatabaseTitle(); } @Override public String getReplicaID() { return ctx.getReplicaID(); } @Override public boolean isDocumentValid() { return ctx.isDocumentValid(); } @Override public long getDocumentSize() { return ctx.getDocumentSize(); } @Override public int getAttachmentCount() { return ctx.getAttachmentCount(); } @Override public List<String> getFolderNames() { return ctx.getFolderNames(); } @Override public boolean lockDocument() { return ctx.lockDocument(); } @Override public boolean unlockDocument() { return ctx.unlockDocument(); } @Override public String getDocumentLockStatus() { return ctx.getDocumentLockStatus(); } @Override public boolean isDocumentLockingEnabled() { return ctx.isDocumentLockingEnabled(); } @Override public String getDomain() { return ctx.getDomain(); } @Override public String getEnvironmentValue(String n) { return ctx.getEnvironmentValue(n); } @Override public void markForDeletion() { ctx.markForDeletion(); } @Override public void unmarkForDeletion() { ctx.unmarkForDeletion(); } @Override public void hardDelete() { ctx.hardDelete(); } @Override public void addToFolder(String n) { ctx.addToFolder(n); } @Override public java.util.List<Number> getTimeZoneOffset(String d) { return ctx.getTimeZoneOffset(d); } @Override public String getCanonicalTimeZone() { return ctx.getCanonicalTimeZone(); } @Override public String timeToTextInZone(String d,String z,String fmt) { return ctx.timeToTextInZone(d,z,fmt); } @Override public String timeZoneToText(String z,String fmt) { return ctx.timeZoneToText(z,fmt); } }; r.add(ev.eval(f, ec)); } return r.isEmpty()?"":r.size()==1?r.get(0):r; });
        functions.put("SET", (ev, args, ctx) -> { String vn = Evaluator.toString(ev.eval(args.get(0),ctx)).toUpperCase(); Object val = ev.eval(args.get(1),ctx); ev.tempScope.get().put(vn, val); return val; });
        functions.put("SETFIELD", (ev, args, ctx) -> { String fn = Evaluator.toString(ev.eval(args.get(0),ctx)).toUpperCase(); Object val = ev.eval(args.get(1),ctx); try { ctx.setField(fn,val); } catch (ContextNotSupportedException e) {} return val; });
        FunctionHandler noop = (ev,args,ctx)->"";
        functions.put("COMMAND", noop);
        functions.put("POSTEDCOMMAND", noop);
    }
}
