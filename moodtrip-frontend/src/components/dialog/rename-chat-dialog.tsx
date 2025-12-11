"use client";

import * as React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

type RenameChatDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialTitle?: string;
  loading?: boolean;
  onConfirm: (newTitle: string) => void | Promise<void>;
};

export function RenameChatDialog({
  open,
  onOpenChange,
  initialTitle = "",
  loading = false,
  onConfirm,
}: RenameChatDialogProps) {
  const [value, setValue] = React.useState(initialTitle);

  React.useEffect(() => {
    if (open) {
      setValue(initialTitle);
    }
  }, [initialTitle, open]);

  const handleConfirm = async () => {
    const trimmed = value.trim();
    if (!trimmed) return;
    await onConfirm(trimmed);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm rounded-2xl border border-slate-100 bg-white/95 p-6 shadow-xl backdrop-blur">
        <DialogHeader>
          <DialogTitle className="text-base font-semibold text-slate-900">
            Rename chat
          </DialogTitle>
        </DialogHeader>

        <div className="mt-3 space-y-2">
          <p className="text-sm text-slate-500">
            Give this conversation a short, clear name.
          </p>
          <input
            className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none ring-0 transition focus:border-slate-400 focus:bg-white focus:ring-2 focus:ring-slate-200"
            placeholder="e.g. Sunday mood walk"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            disabled={loading}
          />
        </div>

        <DialogFooter className="mt-6 flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            className="rounded-full px-4 text-sm"
            onClick={() => onOpenChange(false)}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button
            type="button"
            className="rounded-full px-4 text-sm"
            onClick={handleConfirm}
            disabled={loading || !value.trim()}
          >
            {loading ? "Saving..." : "Save"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
