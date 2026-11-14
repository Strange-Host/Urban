import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LeadTable } from "./LeadList";

const lead = {
  id: 5,
  name: "Amina",
  status: "NEW",
  formName: "Wallet",
  assignedToId: "u1",
  assignedToName: "Jane Doe",
};

const staffOptions = [
  { id: "u1", label: "Jane Doe (STAFF)" },
  { id: "u2", label: "Sam Roe (STAFF)" },
];

function renderTable(props) {
  const noop = () => {};
  return render(
    <LeadTable
      leads={[lead]}
      highlightedLeadId={null}
      registerLeadRef={noop}
      noteEdits={{}}
      focusedNoteId={null}
      followUps={{}}
      onNoteFocus={noop}
      onNoteBlur={noop}
      onNoteChange={noop}
      onSaveNote={noop}
      onStatusChange={noop}
      onFollowUpChange={noop}
      onSaveFollowUp={noop}
      onConvert={noop}
      {...props}
    />
  );
}

describe("lead assignment control", () => {
  it("lets people who can assign pick a different staff member", () => {
    const onAssign = vi.fn();
    renderTable({ canAssign: true, staffOptions, onAssign });

    fireEvent.change(screen.getByLabelText("Assign Amina"), { target: { value: "u2" } });

    expect(onAssign).toHaveBeenCalledWith(lead, "u2");
  });

  it("only shows the assignee to everyone else", () => {
    renderTable({ canAssign: false, staffOptions, onAssign: vi.fn() });

    expect(screen.queryByLabelText("Assign Amina")).toBeNull();
    expect(screen.getByText("Jane Doe")).toBeTruthy();
  });

  it("says Unassigned when nobody owns the lead", () => {
    renderTable({ canAssign: false, leads: [{ ...lead, assignedToId: null, assignedToName: null }] });

    expect(screen.getByText("Unassigned")).toBeTruthy();
  });
});
